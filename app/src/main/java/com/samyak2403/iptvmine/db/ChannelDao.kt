package com.samyak2403.iptvmine.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.samyak2403.iptvmine.adapter.ChannelEntity

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("SELECT * FROM ChannelEntity WHERE playlistId = :playlistId")
    fun getChannelsByPlaylist(playlistId: Int): LiveData<List<ChannelEntity>>

    @Query("SELECT * FROM ChannelEntity")
    fun getAllChannels(): LiveData<List<ChannelEntity>> // Trả về LiveData để cập nhật UI

}
