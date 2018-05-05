package ru.aipova.photogallery

import android.app.Application
import ru.aipova.photogallery.service.GalleryNotificationService

class PhotoGalleryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        GalleryNotificationService.createNotificationChannel(this)
    }
}