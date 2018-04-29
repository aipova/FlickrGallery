package ru.aipova.photogallery.service

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FlickrFetchr {

    private fun getUtlString(urlSpec: String): String {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    fun getUtlBytes(urlSpec: String): ByteArray {
        val url = URL(urlSpec)
        val connection = url.openConnection() as HttpURLConnection
        try {
            return connection.inputStream.use { it.readBytes(1024) }
        } finally {
            connection.disconnect()
        }
    }

    private fun downloadGalleryItems(url: String): MutableList<GalleryItem> {
        val items = mutableListOf<GalleryItem>()
        try {
            val jsonStrong = getUtlString(url)
            Log.i(TAG, "Received JSON: $jsonStrong")
            val response = Gson().fromJson(jsonStrong, FlickrResponse::class.java)
            items.addAll(response.photos.items.filterNot { it.url.isNullOrEmpty() })

        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to fetch items", ioe)
        } catch (je: JSONException) {
            Log.e(TAG, "Failed to parse JSON", je)
        }
        return items
    }

    fun fetchRecent(page: Int = 1): MutableList<GalleryItem> {
        val url = buildFlickrRequestUrl(GET_RECENT, page)
        return downloadGalleryItems(url)
    }

    fun searchPhotos(query: String, page: Int = 1): MutableList<GalleryItem> {
        val url = buildFlickrRequestUrl(SEARCH, page, query)
        return downloadGalleryItems(url)
    }


    private fun buildFlickrRequestUrl(method: String, page: Int, query: String? = null): String {
        val urlBuilder = ENDPOINT.buildUpon()
            .appendQueryParameter("method", method)
            .appendQueryParameter("page", page.toString())
        if (method == SEARCH) {
            urlBuilder.appendQueryParameter("text", query)
        }
        return urlBuilder.build().toString()
    }

    companion object {
        private val TAG = FlickrFetchr::class.java.name
        private const val API_KEY = "88bc56ee304b286682cebdd02f3af505"
        private const val API_PATH = "https://api.flickr.com/services/rest/"
        private const val GET_RECENT = "flickr.photos.getRecent"
        private const val SEARCH = "flickr.photos.search"
        private val ENDPOINT = Uri.parse(API_PATH).buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .appendQueryParameter("per_page", "50")
            .build()
    }
}