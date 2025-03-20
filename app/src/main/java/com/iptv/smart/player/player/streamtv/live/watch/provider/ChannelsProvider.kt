package com.iptv.smart.player.player.streamtv.live.watch.provider

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL
import androidx.core.content.edit
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common

@Serializable
data class RecentChannel(
    val streamUrl: String,
    val timestamp: Long
)

class ChannelsProvider : ViewModel() {

    private val _channels = MutableLiveData<List<Channel>>()
    val channels: LiveData<List<Channel>> = _channels

    private val _filteredChannels = MutableLiveData<List<Channel>>()
    val filteredChannels: LiveData<List<Channel>> = _filteredChannels

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> get() = _isLoading
    private var fetchJob: Job? = null
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var playlistDao: com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistDao
    private lateinit var appContext: Context

    private var cachedChannels: List<Channel> = emptyList()

    fun init(context: Context) {
        Log.d("ChannelsProvider", "Initializing ChannelsProvider")
        appContext = context
        sharedPreferences = context.getSharedPreferences("FavoriteChannels", Context.MODE_PRIVATE)
        playlistDao = AppDatabase.getDatabase(context).playlistDao()
    }

    fun fetchChannelsFromRoom(forceRefresh: Boolean = false) {
        _isLoading.value = true
        Log.d("ChannelsProvider", "fetchChannelsFromRoom called, forceRefresh: $forceRefresh")

        fetchJob?.cancel()
        if (!forceRefresh && cachedChannels.isNotEmpty()) {
            _channels.value = cachedChannels
            _isLoading.value = false
            return
        }

        fetchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                loadChannels()
                withContext(Dispatchers.Main) {
                    _channels.value = cachedChannels
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _isLoading.value = false
                    _error.value = "Failed to load channels: ${e.message}"
                }
            }
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
            if (playlists.isEmpty()) {
                withContext(Dispatchers.Main) {
                    cachedChannels = emptyList()
                    _channels.value = emptyList()
                }
                return
            }

            val allChannels = mutableListOf<Channel>()
            playlists.forEach { playlist ->
                val channels = when (playlist.sourceType) {
                    "URL" -> fetchChannelsFromUrl(playlist.sourcePath)
                    "FILE" -> fetchChannelsFromUri(Uri.parse(playlist.sourcePath))
                    "DEVICE" -> playlist.sourcePath.split(";").map { Uri.parse(it) }.flatMap { fetchChannelsFromUri(it) }
                    else -> emptyList()
                }
                allChannels.addAll(channels)
            }

            val updatedChannels = applyFavoriteStatus(allChannels)
            withContext(Dispatchers.Main) {
                cachedChannels = updatedChannels
                _channels.value = updatedChannels
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                _error.value = "Failed to fetch channels: ${e.message}"
                cachedChannels = emptyList()
                _channels.value = emptyList()
            }
        }
    }

    fun addChannelsFromM3U(channels: List<Channel>) {
        viewModelScope.launch(Dispatchers.Main) {
            val currentList = _channels.value?.toMutableList() ?: mutableListOf()
            val newChannels = channels.map { channel ->
                Log.d("sdfsdf", "addChannelsFromM3U: " + channel.groupTitle)
                channel.copy(
                    isFavorite = isFavorite(channel.streamUrl),
                    groupTitle = channel.groupTitle
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
                val client = OkHttpClient.Builder()
                    .cache(Cache(appContext.cacheDir, 10 * 1024 * 1024))
                    .build()
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()
                val fileText = response.body?.string() ?: return@withContext emptyList()
                parseM3UFile(fileText)
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
        var logoUrl: String? = null
        var streamUrl: String? = null
        var groupTitle: String? = null

        for (line in lines) {
            when {
                line.startsWith("#EXTINF:") -> {
                    val nameMatch = Regex("tvg-name=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                    val groupMatch = Regex("group-title=\"([^\"]+)\"").find(line)?.groupValues?.get(1)
                    val logoMatch = Regex("tvg-logo=\"([^\"]+)\"").find(line)?.groupValues?.get(1)

                    name = nameMatch ?: line.substringAfter(",").trim()
                    groupTitle = groupMatch ?: "Unknown"
                    logoUrl = logoMatch
                    Log.d("ChannelsProvider", "Parsed logoUrl: $logoUrl")
                }
                line.isNotEmpty() -> {
                    streamUrl = line
                    if (!name.isNullOrEmpty() && streamUrl != null) {
                        val finalLogoUrl = logoUrl ?: ""
                        tempChannels.add(
                            Channel(
                                name = name,
                                logoUrl = finalLogoUrl,
                                streamUrl = streamUrl,
                                groupTitle = groupTitle,
                                isFavorite = false
                            )
                        )
                    }
                    name = null
                    logoUrl = null
                    streamUrl = null
                    groupTitle = null
                }
            }
        }
        return tempChannels
    }

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

    fun filterChannels(context: Context, type: String) {
        _channels.value?.let { channelList ->
            val filtered = when (type) {
                "favorite" -> {
                    Log.d("rthtrhrhrhth", "filterChannels: " + Common.getChannels(context))
                    Common.getChannels(context)
                }
                "recent" -> {
                    val recentChannels = getRecentChannels()
                    val sortedRecent = recentChannels
                        .mapNotNull { recent -> channelList.find { it.streamUrl == recent.streamUrl } }
                    sortedRecent
                }
                else -> channelList
            }
            _filteredChannels.value = filtered
        } ?: run {
            _filteredChannels.value = emptyList()
        }
    }

    private fun getRecentChannels(): List<RecentChannel> {
        val recentJson = sharedPreferences.getString("recent_channels_json", "[]") ?: "[]"
        return try {
            Json.decodeFromString<List<RecentChannel>>(recentJson)
                .sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            Log.e("ChannelsProvider", "Error parsing recent channels: ${e.message}")
            emptyList()
        }
    }

    fun toggleFavorite(context: Context, channel: Channel) {
        val listFav: ArrayList<Channel> = ArrayList()
        listFav.addAll(Common.getChannels(context))
        val newChannel = filterChannelsByStreamUrl(listFav, channel.streamUrl)
        if (listFav.contains(newChannel)) {
            listFav.remove(newChannel)
        } else {
            listFav.add(channel)
        }
        Log.d("asdasdsd", "toggleFavorite: $newChannel")
        Common.saveChannels(context, listFav)
    }

    fun removeFavoritesFromDeletedPlaylist(context: Context, playlistSourcePath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val channelsToRemove = when {
                playlistSourcePath.startsWith("http") -> fetchChannelsFromUrl(playlistSourcePath)
                else -> fetchChannelsFromUri(Uri.parse(playlistSourcePath))
            }
            val listFav: ArrayList<Channel> = ArrayList(Common.getChannels(context))
            val streamUrlsToRemove = channelsToRemove.map { it.streamUrl }.toSet()
            val updatedFavList = listFav.filter { it.streamUrl !in streamUrlsToRemove }
            Common.saveChannels(context, updatedFavList.toList())
            _channels.value?.let { currentList ->
                val updatedChannels = currentList.map { channel ->
                    if (channel.streamUrl in streamUrlsToRemove) {
                        channel.copy(isFavorite = false)
                    } else {
                        channel
                    }
                }
                withContext(Dispatchers.Main) {
                    _channels.value = updatedChannels
                    cachedChannels = updatedChannels
                    Log.d("ChannelsProvider", "Removed favorites from deleted playlist: ${streamUrlsToRemove.size}")
                }
            }
        }
    }

    private fun filterChannelsByStreamUrl(channels: List<Channel>, selectedStreamUrl: String): Channel? {
        return channels.find { it.streamUrl == selectedStreamUrl }
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

    private val _shouldRefresh = MutableLiveData<Boolean>()
    val shouldRefresh: LiveData<Boolean> = _shouldRefresh

    fun requestRefresh() {
        _shouldRefresh.postValue(true)
        Log.d("ChannelsProvider", "Requested refresh")
    }

    fun resetRefresh() {
        _shouldRefresh.postValue(false)
    }

    private fun applyFavoriteStatus(channels: List<Channel>): List<Channel> {
        return channels.map {
            val isFavorite = sharedPreferences.getBoolean(it.streamUrl, false)
            it.copy(isFavorite = isFavorite)
        }
    }

    fun addToRecent(context: Context, channel: Channel) {
        viewModelScope.launch(Dispatchers.IO) {
            val recentChannels = getRecentChannels().toMutableList()
            recentChannels.removeAll { it.streamUrl == channel.streamUrl }
            recentChannels.add(RecentChannel(channel.streamUrl, System.currentTimeMillis()))
            if (recentChannels.size > 8) {
                recentChannels.removeAt(0)
            }
            val json = Json.encodeToString(recentChannels)
            sharedPreferences.edit().putString("recent_channels_json", json).apply()
            Log.d("ChannelsProvider", "Added to recent: ${channel.name}, total recent: ${recentChannels.size}")

            withContext(Dispatchers.Main) {
                if (tabLayoutSelectedPosition() == 2) {
                    filterChannels(context, "recent")
                }
            }
        }
    }

    fun isRecent(streamUrl: String): Boolean {
        return getRecentChannels().any { it.streamUrl == streamUrl }
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