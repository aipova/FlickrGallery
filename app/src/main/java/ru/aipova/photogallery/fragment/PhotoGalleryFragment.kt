package ru.aipova.photogallery.fragment


import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ru.aipova.photogallery.R
import ru.aipova.photogallery.model.GalleryItem
import ru.aipova.photogallery.service.FlickrFetchr
import java.lang.ref.WeakReference

class PhotoGalleryFragment : Fragment() {

    private lateinit var photoRecyclerView: RecyclerView
    private var items = mutableListOf<GalleryItem>()

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
        photoRecyclerView.layoutManager = GridLayoutManager(activity, SPAN_COUNT)
        setupAdapter()
        return view
    }

    private fun setupAdapter() {
        if (isAdded) {
            photoRecyclerView.adapter = PhotoAdapter(items)
        }
    }

    inner class PhotoHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val titleTextView = view as TextView

        fun bindGalleryItem(galleryItem: GalleryItem) {
            titleTextView.text = galleryItem.toString()
        }
    }

    inner class PhotoAdapter(var galleryItems: MutableList<GalleryItem>) : RecyclerView.Adapter<PhotoHolder>() {
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

    class FetchItemsTask(fragment: PhotoGalleryFragment) : AsyncTask<Void, Void, MutableList<GalleryItem>>() {
        private val fragmentRef = WeakReference<PhotoGalleryFragment>(fragment)

        override fun doInBackground(vararg params: Void?): MutableList<GalleryItem> {
            return FlickrFetchr().fetchItems()
        }

        override fun onPostExecute(result: MutableList<GalleryItem>?) {
            fragmentRef.get()?.run {
                this.items = result ?: mutableListOf()
                this.setupAdapter()
            }
        }
    }

    companion object {
        const val TAG = "PhotoGalleryFragment"
        const val SPAN_COUNT = 3
        fun newInstance(): PhotoGalleryFragment {
            return PhotoGalleryFragment()
        }
    }
}