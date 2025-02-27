package com.samyak2403.iptvmine.model

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val fileName: String,
    var isFavorite: Boolean = false,
    val groupTitle: String? = null // Thêm groupTitle
)
