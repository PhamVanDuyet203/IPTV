package com.iptv.smart.player.player.streamtv.live.watch.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val channelCount: Int,
    val sourceType: String, // URL, FILE, DEVICE
    val sourcePath: String // URL hoặc đường dẫn file
)