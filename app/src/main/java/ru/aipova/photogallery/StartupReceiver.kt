package ru.aipova.photogallery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import ru.aipova.photogallery.service.PhotoPollStarter.Companion.isPollingOn
import ru.aipova.photogallery.service.PhotoPollStarter.Companion.startPolling

class StartupReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "Received broadcast intent: ${intent?.action}")

        val isAlarmOn = QueryPreferences.isAlarmOn(context)
        if (isAlarmOn && !isPollingOn(context)) startPolling(context)
    }

    companion object {
        private const val TAG = "StartupReceiver"
    }
}