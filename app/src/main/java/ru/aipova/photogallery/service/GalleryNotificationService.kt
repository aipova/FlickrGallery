package ru.aipova.photogallery.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.squareup.picasso.Picasso
import ru.aipova.photogallery.R
import ru.aipova.photogallery.activity.PhotoGalleryActivity

class GalleryNotificationService {
    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "ru.aipova.photogallery.channel.newpictures"
        const val ACTION_SHOW_NOTIFICATION = "ru.aipova.photogallery.SHOW_NOTIFICATION"
        const val PERM_PRIVATE = "ru.aipova.photogallery.PRIVATE"
        const val REQUEST_CODE = "REQUEST_CODE"
        const val NOTIFICATION = "NOTIFICATION"

        fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelName = context.getString(R.string.new_pictures_notifications)
                val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT)
                val notificationManager = context.getSystemService(NotificationManager::class.java)
                notificationManager.createNotificationChannel(channel)
            }
        }

        fun sendNewPicturesNotification(
            ctx: Context,
            result: GalleryItem
        ) {
            val pendingIntent =
                PendingIntent.getActivity(ctx, 0, PhotoGalleryActivity.newIntent(ctx), 0)
            val resources = ctx.resources
            val image = Picasso.get().load(result.url).get()
            val notification = NotificationCompat.Builder(ctx, NOTIFICATION_CHANNEL_ID)
                .setTicker(resources.getString(R.string.new_pictures_title))
                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                .setContentTitle(resources.getString(R.string.new_pictures_title))
                .setContentText(resources.getString(R.string.new_pictures_text))
                .setContentIntent(pendingIntent)
                .setStyle(NotificationCompat.BigPictureStyle().bigPicture(image))
                .setAutoCancel(true)
                .build()
            showBackgroundNotification(ctx, 0, notification)
        }

        fun send(ctx: Context, requestCode: Int, notification: Notification) {
            NotificationManagerCompat.from(ctx).notify(requestCode, notification)
        }

        private fun showBackgroundNotification(ctx: Context, requestCode: Int, notification: Notification) {
            val i = Intent(ACTION_SHOW_NOTIFICATION).apply {
                putExtra(REQUEST_CODE, requestCode)
                putExtra(NOTIFICATION, notification)
            }
            ctx.sendOrderedBroadcast(i, PERM_PRIVATE, null, null, Activity.RESULT_OK, null, null)
        }
    }
}