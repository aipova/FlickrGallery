package ru.aipova.photogallery

import android.support.v4.app.Fragment
import ru.aipova.photogallery.fragment.PhotoGalleryFragment

class PhotoGalleryActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return PhotoGalleryFragment.newInstance()
    }
}