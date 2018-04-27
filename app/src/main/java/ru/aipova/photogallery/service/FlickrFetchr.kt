package ru.aipova.photogallery.service

import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import org.json.JSONException
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class FlickrFetchr {

    fun getUtlString(urlSpec: String): String {
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

    fun fetchItems(page: Int = 1): MutableList<GalleryItem> {
        val items = mutableListOf<GalleryItem>()
        try {
            val url = buildFlickrRequestUrl()
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

    private fun buildFlickrRequestUrl(page: Int = 1): String {
        return Uri.parse(API_PATH)
            .buildUpon()
            .appendQueryParameter("method", "flickr.photos.getRecent")
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .appendQueryParameter("page", page.toString())
            .appendQueryParameter("per_page", "50")
            .build().toString()
    }

    companion object {
        val TAG = FlickrFetchr::class.java.name
        const val API_KEY = "88bc56ee304b286682cebdd02f3af505"
        const val API_PATH = "https://api.flickr.com/services/rest/"
    }
}