package ru.aipova.photogallery

import android.graphics.Bitmap
import android.support.v4.util.LruCache

class BitmapCache {
    companion object {
        private val lruCache: LruCache<String, Bitmap>  = object : LruCache<String, Bitmap>(getCacheSize()) {
            override fun sizeOf(key: String?, value: Bitmap?): Int {
                return (value?.byteCount ?: 0) / 1024
            }
        }

        fun getCacheSize(): Int {
            val maxMemory = Runtime.getRuntime().maxMemory() / 1024
            return (maxMemory / 8).toInt()
        }

        fun getBitmapFromCache(key: String?): Bitmap? {
            return lruCache[key]
        }

        fun cacheContains(key: String?): Boolean {
            return lruCache[key] != null
        }

        fun addBitmapToCache(key: String?, bitmap: Bitmap) {
            if (getBitmapFromCache(key) == null) {
                lruCache.put(key, bitmap)
            }
        }
    }
}