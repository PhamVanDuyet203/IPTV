package com.iptv.smart.player.player.streamtv.live.watch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayout
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()
    private val channelsProvider = ChannelsProvider()

    private val _filteredPlaylists = MutableLiveData<List<PlaylistEntity>>()
    val filteredPlaylists: LiveData<List<PlaylistEntity>> get() = _filteredPlaylists

    init {
        loadAllPlaylists()
        channelsProvider.init(application)
    }


    private fun loadAllPlaylists() {
        viewModelScope.launch(Dispatchers.IO) {
            val playlists = playlistDao.getAllPlaylists()
            _filteredPlaylists.postValue(playlists)
        }
    }

    fun filterPlaylists(sourceType: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (sourceType == null) {
                val playlists = playlistDao.getAllPlaylists()
                _filteredPlaylists.postValue(playlists)
            } else {
                val playlists = playlistDao.getPlaylistsBySourceType(sourceType)
                _filteredPlaylists.postValue(playlists)
            }
        }
    }

    fun updatePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.update(playlist)
            val currentSourceType = when (tabLayout?.selectedTabPosition) {
                0 -> null
                1 -> "URL"
                2 -> "FILE"
                3 -> "DEVICE"
                else -> null
            }
            filterPlaylists(currentSourceType)
        }
    }

    fun deletePlaylist(playlist: PlaylistEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            playlistDao.delete(playlist)

            channelsProvider.removeFavoritesFromDeletedPlaylist(getApplication(), playlist.sourcePath)

            val currentSourceType = when (tabLayout?.selectedTabPosition) {
                0 -> null
                1 -> "URL"
                2 -> "FILE"
                3 -> "DEVICE"
                else -> null
            }
            filterPlaylists(currentSourceType)
        }
    }

    private var tabLayout: TabLayout? = null
    fun setTabLayout(tabLayout: TabLayout) {
        this.tabLayout = tabLayout
    }
}