package com.iptv.smart.player.player.streamtv.live.watch.model

import android.net.Uri

data class VideoItem(
    val uri: Uri,
    val fileName: String,
    var isFavorite: Boolean = false,
    val groupTitle: String? = null // Thêm groupTitle
)
