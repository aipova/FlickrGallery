package ru.aipova.photogallery.activity

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.v4.app.Fragment
import ru.aipova.photogallery.R
import ru.aipova.photogallery.fragment.PhotoPageFragment

class PhotoPageActivity : SingleFragmentActivity() {
    override fun createFragment(): Fragment {
        return PhotoPageFragment.newInstance(intent.data)
    }

    override fun onBackPressed() {
        val fm = supportFragmentManager
        val fragment = fm.findFragmentById(R.id.fragment_container)
        if (fragment is PhotoPageFragment && fragment.canGoBack()) {
            fragment.goBack()
        } else {
            super.onBackPressed()
        }
    }

    companion object {
        fun newIntent(context: Context?, uri: Uri?): Intent {
            return Intent(context, PhotoPageActivity::class.java).apply { data = uri }
        }
    }
}