package com.iptv.smart.player.player.streamtv.live.watch.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.ChannelDao
import com.iptv.smart.player.player.streamtv.live.watch.db.ChannelEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val channelDao: ChannelDao = database.channelDao()

    private val _channels: LiveData<List<ChannelEntity>> = channelDao.getAllChannels()
    val channels: LiveData<List<ChannelEntity>> get() = _channels

    private val _filteredChannels = MutableLiveData<List<ChannelEntity>>()
    val filteredChannels: LiveData<List<ChannelEntity>> get() = _filteredChannels

    private var currentTabPosition: Int = 0

    init {
        // Không cần gọi loadAllChannels vì _channels đã là LiveData từ DAO
    }

    fun setTabPosition(position: Int) {
        currentTabPosition = position
        filterChannels()
    }

    fun insertChannel(channel: ChannelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.insertChannel(channel)
        }
    }

    fun updateChannel(channel: ChannelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.update(channel)
        }
    }

    fun deleteChannel(channel: ChannelEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            channelDao.delete(channel)
        }
    }

    fun filterChannels() {
        viewModelScope.launch(Dispatchers.IO) {
            val allChannels = _channels.value ?: emptyList()
            val filtered = when (currentTabPosition) {
                0 -> allChannels // Tab "All"
                1 -> allChannels.filter { it.isFavorite } // Tab "Favorite"
                2 -> allChannels // Tab "Recent" (chưa có logic, cần thêm nếu có trường thời gian)
                else -> allChannels
            }
            _filteredChannels.postValue(filtered)
        }
    }
}