package ru.aipova.photogallery

import android.app.Activity.RESULT_OK
import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ru.aipova.photogallery.service.GalleryNotificationService

class NotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Received result: $resultCode")
        if (resultCode != RESULT_OK || intent == null || context == null) {
            return
        }

        val requestCode = intent.getIntExtra(GalleryNotificationService.REQUEST_CODE, 0)
        val notification = intent.getParcelableExtra<Notification>(GalleryNotificationService.NOTIFICATION)
        GalleryNotificationService.send(context, requestCode, notification)
    }

    companion object {
        private const val TAG = "NotificationReceiver"
    }
}