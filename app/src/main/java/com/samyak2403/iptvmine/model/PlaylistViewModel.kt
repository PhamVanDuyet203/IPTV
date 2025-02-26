package com.samyak2403.iptvmine.viewmodel

import android.app.Application
import androidx.lifecycle.*
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.db.PlaylistEntity
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
            val playlists = playlistDao.getAllPlaylists() // Call suspend function here
            _filteredPlaylists.postValue(playlists) // Use postValue since we're on a background thread
        }
    }

    fun filterPlaylists(sourceType: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (sourceType == null) {
                val playlists = playlistDao.getAllPlaylists() // Load all if no sourceType
                _filteredPlaylists.postValue(playlists)
            } else {
                val playlists = playlistDao.getPlaylistsBySourceType(sourceType)
                _filteredPlaylists.postValue(playlists)
            }
        }
    }
}