package ru.aipova.photogallery.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import ru.aipova.photogallery.BitmapCache.Companion.addBitmapToCache
import ru.aipova.photogallery.BitmapCache.Companion.getBitmapFromCache
import ru.aipova.photogallery.fragment.PhotoGalleryFragment
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<T>(val responseHandler: Handler) : HandlerThread(TAG) {

    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    var listener: Listener<T>? = null

    interface Listener<T> {
        fun onThumbnailDownloaded(target: T, thumbnail: Bitmap)
    }

    fun clearQueue() {
        responseHandler.removeMessages(MESSAGE_DOWNLOAD)
        requestMap.clear()
    }

    override fun quit(): Boolean {
        hasQuit = true
        return super.quit()
    }

    override fun onLooperPrepared() {
        requestHandler = object : Handler() {
            override fun handleMessage(msg: Message?) {
                if (msg != null && msg.what == MESSAGE_DOWNLOAD && requestMap[msg.obj as T] != null) {
                    Log.i(TAG, "Got request for URL: " + requestMap[msg.obj as T])
                    handleRequest(msg.obj as T)
                }
            }
        }
    }

    private fun handleRequest(target: T) {
        try {
            val url = requestMap[target] ?: return

            var bitmap = getBitmapFromCache(url)

            if (bitmap == null) {
                val bitmapBytes = FlickrFetchr().getUtlBytes(url)
                bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
                Log.i(TAG, "Bitmap created")
                addBitmapToCache(url, bitmap)
            }
            responseHandler.post {
                if (requestMap[target] == url && !hasQuit) {
                    requestMap.remove(target)
                    listener?.onThumbnailDownloaded(target, bitmap!!)
                }
            }
        } catch (ioe: IOException) {
            Log.e(TAG, "Error downloading image", ioe)
        }
    }

    fun queueThumbnail(target: T, url: String?) {
        val type = if (target is PhotoGalleryFragment.PhotoHolder) { "DOWNLOAD"} else {"PRELOAD"}
        Log.i(TAG, "Got a URL for $type: $url")
        if (url == null) {
            requestMap.remove(target)
        } else {
            requestMap[target] = url
            requestHandler.obtainMessage(MESSAGE_DOWNLOAD, target).sendToTarget()
        }
    }

    companion object {
        const val TAG = "ThumbnailDownloader"
        const val MESSAGE_DOWNLOAD = 0
    }
}