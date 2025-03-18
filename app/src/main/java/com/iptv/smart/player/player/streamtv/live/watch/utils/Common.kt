package com.iptv.smart.player.player.streamtv.live.watch.utils

import android.content.Context
import android.content.SharedPreferences
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel

object Common {
    var countInterAdd = 0
    var countInterSelect = 0
    var countInterBackPLay = 0
    var countInterItemPlaylist = 0
    var countInterAddOption = 0

    private const val PREFS_NAME = "ChannelPrefs"
    private const val KEY_CHANNELS = "channels"
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveChannels(context: Context, channels: List<Channel>) {
        val gson = Gson()
        val json = gson.toJson(channels)
        getPrefs(context).edit().putString(KEY_CHANNELS, json).apply()
    }

    fun getChannels(context: Context): List<Channel> {
        val json = getPrefs(context).getString(KEY_CHANNELS, null)
        return if (json != null) {
            val gson = Gson()
            val type = object : TypeToken<List<Channel>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } else {
            emptyList()
        }
    }
}