package com.samyak2403.iptvmine.adapter

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "channels")
data class ChannelEntity(
    @PrimaryKey val streamUrl: String,
    val name: String,
    val logoUrl: String,
    val isFavorite: Boolean
)