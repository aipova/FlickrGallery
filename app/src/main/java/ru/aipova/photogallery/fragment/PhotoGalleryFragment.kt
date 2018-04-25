package ru.aipova.photogallery.fragment


import android.content.res.Resources
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.TextView
import ru.aipova.photogallery.R
import ru.aipova.photogallery.service.FlickrFetchr
import ru.aipova.photogallery.service.GalleryItem
import java.lang.ref.WeakReference

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private var items = mutableListOf<GalleryItem>()
    private var page: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        FetchItemsTask(this).execute()
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
        private val titleTextView = view as TextView

        fun bindGalleryItem(galleryItem: GalleryItem) {
            titleTextView.text = galleryItem.toString()
        }
    }

    inner class PhotoAdapter(var galleryItems: MutableList<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoHolder {
            val textView = TextView(activity)
            return PhotoHolder(textView)
        }

        override fun getItemCount(): Int {
            return galleryItems.size
        }

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {
            holder.bindGalleryItem(galleryItems[position])
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