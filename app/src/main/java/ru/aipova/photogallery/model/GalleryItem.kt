package ru.aipova.photogallery.model

data class GalleryItem(val id: String, val caption: String, val url: String) {
    override fun toString(): String {
        return caption.toUpperCase()
    }
}