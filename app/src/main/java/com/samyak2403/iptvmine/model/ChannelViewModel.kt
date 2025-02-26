package com.samyak2403.iptvmine.model

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.samyak2403.iptvmine.adapter.ChannelEntity
import com.samyak2403.iptvmine.db.AppDatabase
import kotlinx.coroutines.launch

class ChannelViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)


    fun insertChannel(channel: ChannelEntity) {
        viewModelScope.launch {
        }
    }

}
