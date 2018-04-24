package ru.aipova.photogallery.service

import android.net.Uri
import android.util.Log
import org.json.JSONException
import org.json.JSONObject
import ru.aipova.photogallery.model.GalleryItem
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

    fun fetchItems(): MutableList<GalleryItem> {
        val items = mutableListOf<GalleryItem>()
        try {
            val url = buildFlickrRequestUrl()
            val jsonStrong = getUtlString(url)
            Log.i(TAG, "Received JSON: $jsonStrong")
            val jsonBody = JSONObject(jsonStrong)
            parseItems(items, jsonBody)

        } catch (ioe: IOException) {
            Log.e(TAG, "Failed to fetch items", ioe)
        } catch (je: JSONException) {
            Log.e(TAG, "Failed to parse JSON", je)
        }
        return items
    }

    private fun parseItems(items: MutableList<GalleryItem>, jsonBody: JSONObject): Unit {
        val photosObject = jsonBody.getJSONObject("photos")
        val photoArray = photosObject.getJSONArray("photo")
        for (i in 0 until photoArray.length()) {
            val photoObject = photoArray.getJSONObject(i)
            if (photoObject.has("url_s")) {
                val galleryItem = GalleryItem(
                    photoObject.getString("id"),
                    photoObject.getString("title"),
                    photoObject.getString("url_s")
                )
                items.add(galleryItem)
            }
        }
    }

    private fun buildFlickrRequestUrl(): String {
        return Uri.parse(API_PATH)
            .buildUpon()
            .appendQueryParameter("method", "flickr.photos.getRecent")
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s")
            .build().toString()
    }

    companion object {
        val TAG = FlickrFetchr::class.java.name
        const val API_KEY = "88bc56ee304b286682cebdd02f3af505"
        const val API_PATH = "https://api.flickr.com/services/rest/"
    }
}