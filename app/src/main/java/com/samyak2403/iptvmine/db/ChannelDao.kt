package com.samyak2403.iptvmine.db

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface ChannelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: ChannelEntity)

    @Query("SELECT * FROM channels")
    fun getAllChannels(): LiveData<List<ChannelEntity>>

    @Update
    suspend fun update(channel: ChannelEntity)

    @Delete
    suspend fun delete(channel: ChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE streamUrl = :streamUrl")
    suspend fun deleteChannel(streamUrl: String)

    @Query("SELECT isFavorite FROM channels WHERE streamUrl = :streamUrl")
    suspend fun isFavorite(streamUrl: String): Boolean
}