package ru.aipova.photogallery.service

import android.app.AlarmManager
import android.app.AlarmManager.ELAPSED_REALTIME
import android.app.IntentService
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_NO_CREATE
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.SystemClock
import android.util.Log
import ru.aipova.photogallery.PhotoGalleryActivity
import ru.aipova.photogallery.QueryPreferences
import java.util.concurrent.TimeUnit

class PollService : IntentService(TAG) {
    override fun onHandleIntent(intent: Intent?) {
        if (!isNetworkAvailableAndConnected()) {
            return
        }
        val query = QueryPreferences.getStoredQuery(this)
        val lastResultId = QueryPreferences.getLastResultId(this)
        val galleryItems = if (query == null) {
            FlickrFetchr().fetchRecent()
        } else {
            FlickrFetchr().searchPhotos(query)
        }

        if (galleryItems.size == 0) {
            return
        }

        val resultId = galleryItems[0].id
        if (resultId == lastResultId) {
            Log.i(TAG, "Got an old result: $resultId")
        } else {
            Log.i(TAG, "Got a new result: $resultId")

            val pendingIntent = PendingIntent.getActivity(this, 0, PhotoGalleryActivity.newIntent(this), 0)
            GalleryNotificationService.sendNewPicturesNotification(this, pendingIntent)
        }
        QueryPreferences.setLastResultId(this, resultId)

    }

    private fun isNetworkAvailableAndConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isNetworkAvailable = cm.activeNetworkInfo != null
        return isNetworkAvailable && cm.activeNetworkInfo.isConnected
    }

    companion object {
        const val TAG = "PollService"
        private val POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(15)

        fun newIntent(context: Context?): Intent {
            return Intent(context, PollService::class.java)
        }

        fun setServiceAlarm(context: Context?, isOn: Boolean) {
            val intent = newIntent(context)
            val pendingIntent = PendingIntent.getService(context, 0, intent, 0)
            val alarmManager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.run {
                if (isOn) {
                    Log.i(TAG, "poll service started")
                    setRepeating(ELAPSED_REALTIME, SystemClock.elapsedRealtime(), POLL_INTERVAL_MS, pendingIntent)
                } else {
                    cancel(pendingIntent)
                    pendingIntent.cancel()
                }
            }
        }

        fun isServiceAlarmOn(context: Context?): Boolean {
            val pendingIntent = PendingIntent.getService(context, 0, newIntent(context), FLAG_NO_CREATE)
            return pendingIntent != null
        }
     }
}