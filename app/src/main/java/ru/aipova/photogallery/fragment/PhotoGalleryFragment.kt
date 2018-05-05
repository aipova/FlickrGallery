package ru.aipova.photogallery.fragment


import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SearchView
import android.util.Log
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.ImageView
import android.widget.ProgressBar
import ru.aipova.photogallery.BitmapCache.Companion.cacheContains
import ru.aipova.photogallery.BitmapCache.Companion.getBitmapFromCache
import ru.aipova.photogallery.QueryPreferences
import ru.aipova.photogallery.QueryPreferences.Companion.getStoredQuery
import ru.aipova.photogallery.R
import ru.aipova.photogallery.service.FlickrFetchr
import ru.aipova.photogallery.service.GalleryItem
import ru.aipova.photogallery.service.PollService
import ru.aipova.photogallery.service.ThumbnailDownloader
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private var items = mutableListOf<GalleryItem>()
    private var currentPage: Int = 1
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var thumbnailPreloader: ThumbnailDownloader<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        setHasOptionsMenu(true)
        updateItems()
        setupImageLoaders(Handler())

        Log.i(TAG, "Background threads started")
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater?.inflate(R.menu.fragment_photo_gallery, menu)

        val searchView = menu?.findItem(R.id.menu_item_search)?.actionView as SearchView
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                Log.i(TAG, "Query text submit: $query")
                QueryPreferences.setStoredQuery(activity, query)
                clearAndUpdateItems()
                searchView.onActionViewCollapsed()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                Log.i(TAG, "Query text change: $newText")
                return false
            }

        })
        searchView.setOnSearchClickListener {
            searchView.setQuery(QueryPreferences.getStoredQuery(activity), false)
        }

        val togglePollItem = menu.findItem(R.id.menu_item_toggle_polling)
        if (PollService.isServiceAlarmOn(activity)) {
            togglePollItem.setTitle(R.string.stop_polling)
        } else {
            togglePollItem.setTitle(R.string.start_polling)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?) = when (item?.itemId) {
        R.id.menu_item_clear -> {
            QueryPreferences.setStoredQuery(activity, null)
            clearAndUpdateItems()
            true
        }
        R.id.menu_item_toggle_polling -> {
            val shouldStartAlarm = !PollService.isServiceAlarmOn(activity)
            PollService.setServiceAlarm(activity, shouldStartAlarm)
            activity?.invalidateOptionsMenu()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }


    private fun clearAndUpdateItems() {
        currentPage = 1
        items.clear()
        photoRecyclerView.adapter.notifyDataSetChanged()
        photoRecyclerView.scrollToPosition(0)
        progressBar.visibility = VISIBLE
        updateItems()
    }

    private fun updateItems() {
        FetchItemsTask(this, getStoredQuery(activity)).execute()
    }

    private fun setupImageLoaders(responseHandler: Handler) {
        setupImageDownloader(responseHandler)
        setupImagePreloader(responseHandler)
    }

    private fun setupImageDownloader(responseHandler: Handler) {
        thumbnailDownloader = ThumbnailDownloader<PhotoHolder>(responseHandler).apply {
            listener = object : ThumbnailDownloader.Listener<PhotoHolder> {
                override fun onThumbnailDownloaded(target: PhotoHolder, thumbnail: Bitmap) {
                    target.bindDrawable(BitmapDrawable(resources, thumbnail))
                }
            }
            start()
            looper
        }
    }

    private fun setupImagePreloader(responseHandler: Handler) {
        thumbnailPreloader = ThumbnailDownloader<String>(responseHandler).apply {
            priority = thumbnailDownloader.priority - 1
            start()
            looper
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        quitLoaders()
        Log.i(TAG, "Background thread destroyed")
    }

    private fun quitLoaders() {
        thumbnailDownloader.quit()
        thumbnailPreloader.quit()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        progressBar = view.findViewById(R.id.progress_bar)
        setupRecyclerView(view)
        setupAdapter()
        return view
    }

    private fun setupRecyclerView(view: View) {
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        with(photoRecyclerView) {
            setHasFixedSize(true)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                    if (!recyclerView!!.canScrollVertically(1)) {
                        updateItems()
                    }
                }
            })
            viewTreeObserver.addOnGlobalLayoutListener(object :
                ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    val spanCount = photoRecyclerView.width / (getDensity() * SPAN_DP_SIZE).toInt()
                    photoRecyclerView.layoutManager = GridLayoutManager(activity, spanCount)
                    photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clearLoadersQueue()
    }

    private fun clearLoadersQueue() {
        thumbnailDownloader.clearQueue()
        thumbnailPreloader.clearQueue()
    }

    private fun getDensity() = Resources.getSystem().displayMetrics.density

    private fun setupAdapter(positionStart: Int = 0, itemsCount: Int = 0) {
        if (isAdded) {
            progressBar.visibility = GONE
            if (photoRecyclerView.adapter == null) {
                photoRecyclerView.adapter = PhotoAdapter(items)
            } else {
                photoRecyclerView.adapter.notifyItemRangeInserted(positionStart, itemsCount)
            }
        }
    }

    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemImageView = view.findViewById<ImageView>(R.id.item_image_view)
        var imageUrl: String? = null

        fun bindDrawable(drawable: Drawable) {
            itemImageView.setImageDrawable(drawable)
        }

        fun unbindDrawable() {
            itemImageView.setImageDrawable(null)
        }

        fun bindImageUrl(url: String?) {
            imageUrl = url
        }
    }

    inner class PhotoAdapter(var galleryItems: MutableList<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val view = LayoutInflater.from(activity).inflate(R.layout.gallery_item, parent, false)
            return PhotoHolder(view)
        }

        override fun getItemCount(): Int {
            return galleryItems.size
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            val galleryItem = galleryItems[position]
            holder.bindImageUrl(galleryItem.url)
            val bitmapFromCache = getBitmapFromCache(galleryItem.url)
            if (bitmapFromCache != null) {
                holder.bindDrawable(BitmapDrawable(resources, bitmapFromCache))
            } else {
                thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
            }
            PrefetchImagesTask(this@PhotoGalleryFragment).execute(position)
        }

        override fun onViewRecycled(holder: PhotoHolder) {
            holder.unbindDrawable()
        }
    }

    class PrefetchImagesTask(fragment: PhotoGalleryFragment) :
        AsyncTask<Int, Void, Unit>() {
        private val fragmentRef = WeakReference<PhotoGalleryFragment>(fragment)

        override fun doInBackground(vararg params: Int?) {
            fragmentRef.get()?.run {
                val position = params[0] ?: return
                for (i in fetchRange(position)) {
                    val itemUrl = items[i].url
                    if (itemUrl != null && !cacheContains(itemUrl)) {
                        thumbnailPreloader.queueThumbnail(itemUrl, itemUrl)
                    }
                }
            }
        }

        private fun PhotoGalleryFragment.fetchRange(position: Int): IntRange {
            return max(0, position - PREFETCH_COUNT) until
                    min(items.size, position + PREFETCH_COUNT)
        }
    }

    class FetchItemsTask(
        fragment: PhotoGalleryFragment,
        val storedQuery: String?
    ) :
        AsyncTask<Void, Void, MutableList<GalleryItem>>() {
        private val fragmentRef = WeakReference<PhotoGalleryFragment>(fragment)

        override fun doInBackground(vararg params: Void?): MutableList<GalleryItem> {
            val page = getAndUpdateCurrentPage()
            return if (storedQuery == null) {
                FlickrFetchr().fetchRecent(page)
            } else {
                FlickrFetchr().searchPhotos(storedQuery, page)
            }
        }

        private fun getAndUpdateCurrentPage(): Int {
            return fragmentRef.get()?.let {
                it.currentPage++
            } ?: 1
        }

        override fun onPostExecute(result: MutableList<GalleryItem>?) {
            fragmentRef.get()?.run {
                val insertingPosition = items.size
                val addedAmount = if (result != null) {
                    items.addAll(result)
                    result.size
                } else {
                    0
                }
                setupAdapter(insertingPosition, addedAmount)
            }
        }
    }


    companion object {
        const val TAG = "PhotoGalleryFragment"
        const val SPAN_DP_SIZE = 200
        const val PREFETCH_COUNT = 10
        fun newInstance(): PhotoGalleryFragment {
            return PhotoGalleryFragment()
        }
    }
}