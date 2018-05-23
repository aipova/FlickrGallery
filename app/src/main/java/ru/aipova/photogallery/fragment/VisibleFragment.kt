package ru.aipova.photogallery.fragment


import android.app.Activity.RESULT_CANCELED
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.app.Fragment
import android.util.Log
import ru.aipova.photogallery.service.GalleryNotificationService.Companion.ACTION_SHOW_NOTIFICATION
import ru.aipova.photogallery.service.GalleryNotificationService.Companion.PERM_PRIVATE

open class VisibleFragment : Fragment() {

    override fun onStart() {
        super.onStart()
        activity?.registerReceiver(onShowNotification, IntentFilter(ACTION_SHOW_NOTIFICATION), PERM_PRIVATE, null)
    }

    override fun onStop() {
        super.onStop()
        activity?.unregisterReceiver(onShowNotification)
    }

    private val onShowNotification = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "cancelling notifcation")
            resultCode = RESULT_CANCELED
        }
    }

    companion object {
        const val TAG = "VisibleFragment"
    }
}