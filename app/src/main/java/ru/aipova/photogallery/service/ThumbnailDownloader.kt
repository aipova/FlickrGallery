package ru.aipova.photogallery.service

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.HandlerThread
import android.os.Message
import android.util.Log
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

class ThumbnailDownloader<T>(val responseHandler: Handler) : HandlerThread(TAG) {

    private var hasQuit = false
    private lateinit var requestHandler: Handler
    private val requestMap = ConcurrentHashMap<T, String>()
    var thumbnailDownloadListener: ThumbnailDownloadListener<T>? = null

    interface ThumbnailDownloadListener<T> {
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
                if (msg != null && msg.what == MESSAGE_DOWNLOAD) {
                    Log.i(TAG, "Got request for URL: " + requestMap[msg.obj as T])
                    handleRequest(msg.obj as T)
                }
            }
        }
    }

    private fun handleRequest(target: T) {
        try {
            val url = requestMap[target] ?: return

            val bitmapBytes = FlickrFetchr().getUtlBytes(url)
            val bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.size)
            Log.i(TAG, "Bitmap created")

            responseHandler.post {
                if (requestMap[target] == url && !hasQuit) {
                    requestMap.remove(target)
                    thumbnailDownloadListener?.onThumbnailDownloaded(target, bitmap)
                }
            }
        } catch (ioe: IOException) {
            Log.e(TAG, "Error downloading image", ioe)
        }
    }

    fun queueThumbnail(target: T, url: String?) {
        Log.i(TAG, "Got a URL: $url")
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