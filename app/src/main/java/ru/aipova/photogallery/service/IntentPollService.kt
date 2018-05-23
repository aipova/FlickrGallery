package ru.aipova.photogallery.service

import android.app.IntentService
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager

class IntentPollService : IntentService(TAG) {
    override fun onHandleIntent(intent: Intent?) {
        if (!isNetworkAvailableAndConnected()) {
            return
        }
        PhotoChecker.checkAndNotifyAboutNewPhotos(this)
    }

    private fun isNetworkAvailableAndConnected(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val isNetworkAvailable = cm.activeNetworkInfo != null
        return isNetworkAvailable && cm.activeNetworkInfo.isConnected
    }

    companion object {
        const val TAG = "IntentPollService"

        fun newIntent(context: Context?): Intent {
            return Intent(context, IntentPollService::class.java)
        }
     }
}