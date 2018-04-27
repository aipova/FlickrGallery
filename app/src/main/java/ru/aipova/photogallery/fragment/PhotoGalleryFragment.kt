package ru.aipova.photogallery.fragment


import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.util.LruCache
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import ru.aipova.photogallery.R
import ru.aipova.photogallery.service.FlickrFetchr
import ru.aipova.photogallery.service.GalleryItem
import ru.aipova.photogallery.service.ThumbnailDownloader
import java.lang.ref.WeakReference

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private var items = mutableListOf<GalleryItem>()
    private var page: Int = 1
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var lruCache: LruCache<String, Bitmap>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        FetchItemsTask(this).execute()

        val maxMemory = Runtime.getRuntime().maxMemory() / 1024
        val cacheSize = maxMemory / 8
        lruCache = object : LruCache<String, Bitmap>(cacheSize.toInt()) {
            override fun sizeOf(key: String?, value: Bitmap?): Int {
                return (value?.byteCount ?: 0) / 1024
            }
        }

        val responseHandler = Handler()
        thumbnailDownloader = ThumbnailDownloader(responseHandler)
        thumbnailDownloader.thumbnailDownloadListener = object : ThumbnailDownloader.ThumbnailDownloadListener<PhotoHolder> {
            override fun onThumbnailDownloaded(target: PhotoHolder, thumbnail: Bitmap) {
                target.bindDrawable(BitmapDrawable(resources, thumbnail))
                addBitmapToCache(target.imageId, thumbnail)
            }
        }
        thumbnailDownloader.start()
        thumbnailDownloader.looper
        Log.i(TAG, "Background thread started")
    }

    fun getBitmapFromCache(key: String?): Bitmap? {
        return lruCache[key]
    }

    fun addBitmapToCache(key: String?, bitmap: Bitmap) {
        if (getBitmapFromCache(key) == null) {
            lruCache.put(key, bitmap)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        thumbnailDownloader.quit()
        Log.i(TAG, "Backgroun thread destroyed")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                if (!recyclerView!!.canScrollVertically(1)) {
                    FetchItemsTask(this@PhotoGalleryFragment).execute()
                }
            }
        })
        photoRecyclerView.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val spanCount = photoRecyclerView.width / (getDensity() * SPAN_DP_SIZE).toInt()
                photoRecyclerView.layoutManager = GridLayoutManager(activity, spanCount)
                photoRecyclerView.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
        setupAdapter()
        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        thumbnailDownloader.clearQueue()
    }

    private fun getDensity() = Resources.getSystem().displayMetrics.density

    private fun setupAdapter() {
        if (isAdded) {
            if (photoRecyclerView.adapter == null) {
                photoRecyclerView.adapter = PhotoAdapter(items)
            } else {
                photoRecyclerView.adapter.notifyDataSetChanged()
            }
        }
    }

    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val itemImageView = view.findViewById<ImageView>(R.id.item_image_view)
        var imageId: String? = null

        fun bindDrawable(drawable: Drawable) {
            itemImageView.setImageDrawable(drawable)
        }

        fun bindId(id: String?) {
            imageId = id
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
            holder.bindId(galleryItem.id)
            val bitmapFromCache = getBitmapFromCache(galleryItem.id)
            if (bitmapFromCache != null) {
                holder.bindDrawable(BitmapDrawable(resources, bitmapFromCache))
            } else {
                val placeholder = resources.getDrawable(R.drawable.bill_up_close)
                holder.bindDrawable(placeholder)
                thumbnailDownloader.queueThumbnail(holder, galleryItem.url)
            }
        }
    }

    class FetchItemsTask(fragment: PhotoGalleryFragment) :
        AsyncTask<Void, Void, MutableList<GalleryItem>>() {
        private val fragmentRef = WeakReference<PhotoGalleryFragment>(fragment)

        override fun doInBackground(vararg params: Void?): MutableList<GalleryItem> {
            val page = fragmentRef.get()?.page ?: 1
            val items = FlickrFetchr().fetchItems(page)
            fragmentRef.get()?.page = page + 1
            return items
        }

        override fun onPostExecute(result: MutableList<GalleryItem>?) {
            fragmentRef.get()?.run {
                this.items.addAll(result ?: mutableListOf())
                this.setupAdapter()
            }
        }
    }

    companion object {
        const val TAG = "PhotoGalleryFragment"
        const val SPAN_DP_SIZE = 120
        const val SPAN_COUNT = 3
        fun newInstance(): PhotoGalleryFragment {
            return PhotoGalleryFragment()
        }
    }
}