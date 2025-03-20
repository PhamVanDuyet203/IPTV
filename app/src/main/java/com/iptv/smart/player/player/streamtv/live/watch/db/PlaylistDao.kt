package com.iptv.smart.player.player.streamtv.live.watch.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Delete

@Dao
interface PlaylistDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists")
    suspend fun getAllPlaylists(): List<PlaylistEntity>

    @Query("SELECT * FROM playlists WHERE sourceType = :sourceType")
    suspend fun getPlaylistsBySourceType(sourceType: String): List<PlaylistEntity>

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: PlaylistEntity)

    @Query("SELECT * FROM playlists WHERE name = :name LIMIT 1")
    suspend fun getPlaylistByName(name: String): PlaylistEntity?


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Query("SELECT COUNT(*) FROM playlists")
    suspend fun getPlaylistCount(): Int


}