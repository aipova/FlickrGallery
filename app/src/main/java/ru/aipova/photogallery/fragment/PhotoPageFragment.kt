package ru.aipova.photogallery.fragment

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import ru.aipova.photogallery.R

class PhotoPageFragment : VisibleFragment() {
    private var uri: Uri? = null
    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uri = arguments?.getParcelable(ARG_URI)
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_page, container, false)
        progressBar = view.findViewById<ProgressBar>(R.id.progress_bar).apply { max = MAX_PROGRESS }

        webView = view.findViewById(R.id.web_view)
        with(webView) {
            settings.javaScriptEnabled = true
            webChromeClient = webChromeClientImpl
            webViewClient = webViewClientImpl
            loadUrl(uri.toString())
        }

        return view
    }

    private val webChromeClientImpl = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            if (newProgress == MAX_PROGRESS) {
                progressBar.visibility = GONE
            } else {
                progressBar.visibility = VISIBLE
                progressBar.progress = newProgress
            }
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            (activity as AppCompatActivity).supportActionBar?.subtitle = title
        }
    }

    private val webViewClientImpl = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
            return handleRequest(Uri.parse(url))
        }

        @TargetApi(Build.VERSION_CODES.N)
        override fun shouldOverrideUrlLoading(
            view: WebView?,
            request: WebResourceRequest?
        ): Boolean {
            return handleRequest(request?.url)
        }

        fun handleRequest(uri: Uri?): Boolean {
            return if (HTTP_SCHEMES.contains(uri?.scheme)) {
                false
            } else {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
                true
            }

        }
    }

    fun canGoBack(): Boolean {
        return webView.canGoBack()
    }

    fun goBack() {
        webView.goBack()
    }

    companion object {
        private const val ARG_URI = "photo_page_uri"
        private const val MAX_PROGRESS = 100
        private val HTTP_SCHEMES = listOf("http", "https")

        fun newInstance(uri: Uri) : PhotoPageFragment {
            val args = Bundle().apply { putParcelable(ARG_URI, uri) }
            return PhotoPageFragment().apply {
                arguments = args
            }
        }
    }
}