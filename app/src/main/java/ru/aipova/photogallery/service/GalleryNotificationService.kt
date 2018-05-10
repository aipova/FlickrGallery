package ru.aipova.photogallery.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import ru.aipova.photogallery.PhotoGalleryActivity
import ru.aipova.photogallery.R

class GalleryNotificationService {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ru.aipova.photogallery.channel.newpictures"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = context.getString(R.string.new_pictures_notifications)
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun sendNewPicturesNotification(ctx: Context) {
            val pendingIntent =
                PendingIntent.getActivity(ctx, 0, PhotoGalleryActivity.newIntent(ctx), 0)
            val resources = ctx.resources
            val notification = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(ctx).notify(0, notification)
        }
    }
}