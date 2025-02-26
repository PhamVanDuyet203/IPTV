package com.samyak2403.iptvmine.provider

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ChannelsProvider : ViewModel() {

    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>> = _channels

    private val _filteredChannels = MutableLiveData<List<Channel>>()
    val filteredChannels: LiveData<List<Channel>> = _filteredChannels

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var fetchJob: Job? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var playlistDao: com.samyak2403.iptvmine.db.PlaylistDao
    private lateinit var appContext: Context

    fun init(context: Context) {
        Log.d("ChannelsProvider", "Initializing ChannelsProvider")
        appContext = context
        sharedPreferences = context.getSharedPreferences("FavoriteChannels", Context.MODE_PRIVATE)
        playlistDao = AppDatabase.getDatabase(context).playlistDao()
    }

    fun fetchChannelsFromRoom() {
        Log.d("ChannelsProvider", "fetchChannelsFromRoom called")
        fetchJob?.cancel()
        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            loadChannels()
        }
    }

    suspend fun refreshChannels() {
        Log.d("ChannelsProvider", "refreshChannels called")
        withContext(Dispatchers.IO) {
            loadChannels()
        }
    }

    private suspend fun loadChannels() {
        Log.d("ChannelsProvider", "Loading channels")
        try {
            val playlists = playlistDao.getAllPlaylists()
            Log.d("ChannelsProvider", "Fetched playlists: ${playlists.size}")
            if (playlists.isEmpty()) {
                withContext(Dispatchers.Main) {
                    _error.value = "No playlists found in database. Please add a playlist."
                    _channels.value = emptyList()
                    Log.d("ChannelsProvider", "No playlists, set empty channels")
                }
                return
            }

            val allChannels = mutableListOf<Channel>()
            playlists.forEach { playlist ->
                val channels = when (playlist.sourceType) {
                    "URL" -> fetchChannelsFromUrl(playlist.sourcePath)
                    "FILE" -> fetchChannelsFromUri(Uri.parse(playlist.sourcePath))
                    "DEVICE" -> {
                        val uriList = playlist.sourcePath.split(";").map { Uri.parse(it) }
                        uriList.flatMap { fetchChannelsFromUri(it) }
                    }
                    else -> emptyList()
                }
                Log.d("ChannelsProvider", "Channels from ${playlist.sourceType}: ${channels.size}")
                allChannels.addAll(channels)
            }

            val updatedChannels = applyFavoriteStatus(allChannels)
            withContext(Dispatchers.Main) {
                _channels.value = updatedChannels
                Log.d("ChannelsProvider", "Updated channels: ${updatedChannels.size}")
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to fetch channels: ${e.message}"
                _channels.value = emptyList()
                Log.e("ChannelsProvider", "Error fetching channels: ${e.message}")
            }
        }
    }

    // Thêm hàm để thêm kênh từ parseM3U vào danh sách chung
    fun addChannelsFromM3U(channels: List<Channel>) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentList = _channels.value?.toMutableList() ?: mutableListOf()
            val newChannels = channels.map { channel ->
                channel.copy(
                    isFavorite = isFavorite(channel.streamUrl),
                    groupTitle = channel.groupTitle // Giữ groupTitle từ parseM3U
                )
            }
            currentList.addAll(newChannels.filter { newChannel ->
                currentList.none { it.streamUrl == newChannel.streamUrl }
            })
            _channels.value = applyFavoriteStatus(currentList)
            Log.d("ChannelsProvider", "Added channels from M3U: ${newChannels.size}, total now: ${currentList.size}")
        }
    }

    private suspend fun fetchChannelsFromUrl(url: String): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                val urlConnection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 10000
                    readTimeout = 10000
                }
                urlConnection.inputStream.bufferedReader().use { reader ->
                    val fileText = reader.readText()
                    parseM3UFile(fileText)
                }
            } catch (e: Exception) {
                Log.e("ChannelsProvider", "Error fetching from URL $url: ${e.message}")
                emptyList()
            }
        }
    }

    private suspend fun fetchChannelsFromUri(uri: Uri): List<Channel> {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = appContext.contentResolver.openInputStream(uri)
                val reader = BufferedReader(InputStreamReader(inputStream))
                val fileText = reader.readText()
                reader.close()
                parseM3UFile(fileText)
            } catch (e: Exception) {
                Log.e("ChannelsProvider", "Error fetching from URI $uri: ${e.message}")
                emptyList()
            }
        }
    }

    private fun parseM3UFile(fileText: String): List<Channel> {
        val lines = fileText.split("\n")
        val tempChannels = mutableListOf<Channel>()
        var name: String? = null
        var logoUrl: String = getDefaultLogoUrl()
        var streamUrl: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    name = extractChannelName(line)
                    logoUrl = extractLogoUrl(line) ?: getDefaultLogoUrl()
                }
                line.isNotEmpty() -> {
                    streamUrl = line
                    if (!name.isNullOrEmpty() && streamUrl != null) {
                        tempChannels.add(
                            Channel(
                                name = name,
                                logoUrl = logoUrl,
                                streamUrl = streamUrl,
                                isFavorite = false
                            )
                        )
                    }
                    name = null
                    logoUrl = getDefaultLogoUrl()
                    streamUrl = null
                }
            }
        }
        return tempChannels
    }

    private fun getDefaultLogoUrl(): String = "assets/images/ic_tv.png"

    private fun extractChannelName(line: String): String? = line.split(",").lastOrNull()?.trim()

    private fun extractLogoUrl(line: String): String? {
        val parts = line.split("\"")
        return when {
            parts.size > 1 && isValidUrl(parts[1]) -> parts[1]
            parts.size > 5 && isValidUrl(parts[5]) -> parts[5]
            else -> null
        }
    }

    private fun isValidUrl(url: String): Boolean = url.startsWith("https") || url.startsWith("http")

    fun filterChannels(type: String) {
        Log.d("ChannelsProvider", "Filtering channels by type: $type")
        _channels.value?.let { channelList ->
            val filtered = when (type) {
                "favorite" -> channelList.filter { it.isFavorite }
                "recent" -> channelList.filter { isRecent(it.streamUrl) }
                else -> channelList
            }
            _filteredChannels.value = filtered
            Log.d("ChannelsProvider", "Filtered channels ($type): ${filtered.size}")
        } ?: run {
            _filteredChannels.value = emptyList()
            Log.d("ChannelsProvider", "No channels to filter, set empty list")
        }
    }

    fun toggleFavorite(channel: Channel) {
        Log.d("ChannelsProvider", "Toggling favorite for channel: ${channel.name}")
        _channels.value?.let { currentList ->
            val updatedList = currentList.map {
                if (it.streamUrl == channel.streamUrl) {
                    val newFavoriteStatus = !it.isFavorite
                    sharedPreferences.edit().putBoolean(it.streamUrl, newFavoriteStatus).apply()
                    it.copy(isFavorite = newFavoriteStatus)
                } else it
            }
            _channels.value = updatedList
            Log.d("ChannelsProvider", "Toggled favorite, updated channels: ${updatedList.size}")
            if (tabLayoutSelectedPosition() == 1) {
                _filteredChannels.value = updatedList.filter { it.isFavorite }
                Log.d("ChannelsProvider", "Updated filtered channels (favorite): ${_filteredChannels.value?.size}")
            }
        }
    }

    fun updateChannel(updatedChannel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("ChannelsProvider", "Updating channel: ${updatedChannel.name}")
            _channels.value?.let { currentList ->
                val updatedList = currentList.map {
                    if (it.streamUrl == updatedChannel.streamUrl) updatedChannel else it
                }
                withContext(Dispatchers.Main) {
                    _channels.value = updatedList
                    Log.d("ChannelsProvider", "Channel updated in LiveData: ${updatedChannel.name}")
                    if (tabLayoutSelectedPosition() == 1) {
                        _filteredChannels.value = updatedList.filter { it.isFavorite }
                    }
                }
            }
        }
    }

    fun deleteChannel(channelToDelete: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("ChannelsProvider", "Deleting channel: ${channelToDelete.name}")
            _channels.value?.let { currentList ->
                val updatedList = currentList.filter { it.streamUrl != channelToDelete.streamUrl }
                withContext(Dispatchers.Main) {
                    _channels.value = updatedList
                    Log.d("ChannelsProvider", "Channel deleted from LiveData: ${channelToDelete.name}")
                    if (tabLayoutSelectedPosition() == 1) {
                        _filteredChannels.value = updatedList.filter { it.isFavorite }
                    } else if (tabLayoutSelectedPosition() == 2) {
                        _filteredChannels.value = updatedList.filter { isRecent(it.streamUrl) }
                    }
                }
            }
        }
    }

    private fun applyFavoriteStatus(channels: List<Channel>): List<Channel> {
        return channels.map {
            val isFavorite = sharedPreferences.getBoolean(it.streamUrl, false)
            it.copy(isFavorite = isFavorite)
        }
    }

    fun addToRecent(channel: Channel) {
        val recentSet = sharedPreferences.getStringSet("recent_channels", emptySet())?.toMutableSet() ?: mutableSetOf()
        recentSet.add(channel.streamUrl)
        if (recentSet.size > 10) {
            recentSet.remove(recentSet.first())
        }
        sharedPreferences.edit().putStringSet("recent_channels", recentSet).apply()
    }

    private fun isRecent(streamUrl: String): Boolean {
        val recentSet = sharedPreferences.getStringSet("recent_channels", emptySet()) ?: emptySet()
        return recentSet.contains(streamUrl)
    }

    fun isFavorite(streamUrl: String): Boolean {
        return sharedPreferences.getBoolean(streamUrl, false)
    }

    private var currentTabPosition = 0
    fun setTabPosition(position: Int) {
        currentTabPosition = position
        Log.d("ChannelsProvider", "Tab position set to: $position")
    }

    private fun tabLayoutSelectedPosition(): Int = currentTabPosition

    override fun onCleared() {
        Log.d("ChannelsProvider", "ChannelsProvider cleared")
        super.onCleared()
        fetchJob?.cancel()
    }
}