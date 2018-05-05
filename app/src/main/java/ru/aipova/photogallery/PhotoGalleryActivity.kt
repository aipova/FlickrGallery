package ru.aipova.photogallery

import android.content.Context
import android.content.Intent
import android.support.v4.app.Fragment
import ru.aipova.photogallery.fragment.PhotoGalleryFragment

class PhotoGalleryActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return PhotoGalleryFragment.newInstance()
    }

    companion object {
        fun newIntent(context: Context?): Intent {
            return Intent(context, PhotoGalleryActivity::class.java)
        }
    }
}