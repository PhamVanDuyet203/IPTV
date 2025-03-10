package com.iptv.smart.player.player.streamtv.live.watch.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val logoUrl: String,
    val isFavorite: Boolean
)