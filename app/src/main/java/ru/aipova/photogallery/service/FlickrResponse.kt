package ru.aipova.photogallery.service

import com.google.gson.annotations.SerializedName

data class FlickrResponse(val photos: GalleryResult, val stat: String)

data class GalleryResult(val page: Int, val pages: Int, val perpage: Int, val total: Int, @SerializedName("photo") val items: List<GalleryItem>)

data class GalleryItem(val id: String, @SerializedName("title") val caption: String, @SerializedName("url_s") val url: String?) {
    override fun toString(): String {
        return caption.toUpperCase()
    }
}