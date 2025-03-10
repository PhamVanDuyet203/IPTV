package com.iptv.smart.player.player.streamtv.live.watch.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.google.android.material.tabs.TabLayout
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val playlistDao = AppDatabase.getDatabase(application).playlistDao()

    private val _filteredPlaylists = MutableLiveData<List<PlaylistEntity>>()
    val filteredPlaylists: LiveData<List<PlaylistEntity>> get() = _filteredPlaylists

    init {
        loadAllPlaylists()
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
            playlistDao.update(playlist) // Cập nhật playlist trong DB
            // Sau khi cập nhật, làm mới danh sách dựa trên filter hiện tại
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
            playlistDao.delete(playlist) // Xóa playlist khỏi DB
            // Sau khi xóa, làm mới danh sách dựa trên filter hiện tại
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

    // Để lấy được selectedTabPosition, cần thêm một cách để truy cập từ HomePageFragment
    private var tabLayout: TabLayout? = null
    fun setTabLayout(tabLayout: TabLayout) {
        this.tabLayout = tabLayout
    }
}