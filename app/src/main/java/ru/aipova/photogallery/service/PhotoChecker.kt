package ru.aipova.photogallery.service

import android.content.Context
import android.util.Log
import ru.aipova.photogallery.QueryPreferences

class PhotoChecker private constructor() {

    companion object {
        private const val TAG = "PhotoCheckAndNotifier"

        fun checkAndNotifyAboutNewPhotos(context: Context) {
            val galleryItems = fetchGalleryItems(context)
            if (galleryItems.isEmpty()) {
                return
            }

            val resultId = galleryItems.first().id
            val result = galleryItems.first()
            if (resultId == getLastResultId(context)) {
                Log.i(TAG, "Got an old result: $resultId")
            } else {
                Log.i(TAG, "Got a new result: $resultId")
                GalleryNotificationService.sendNewPicturesNotification(context, result)
                updateLastResultId(context, resultId)
            }
        }

        private fun fetchGalleryItems(context: Context): MutableList<GalleryItem> {
            val query = QueryPreferences.getStoredQuery(context)
            return if (query == null) {
                FlickrFetchr().fetchRecent()
            } else {
                FlickrFetchr().searchPhotos(query)
            }
        }

        private fun getLastResultId(context: Context) =
            QueryPreferences.getLastResultId(context)

        private fun updateLastResultId(context: Context, resultId: String) {
            QueryPreferences.setLastResultId(context, resultId)
        }
    }
}