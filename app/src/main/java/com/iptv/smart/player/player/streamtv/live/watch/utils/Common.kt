package com.iptv.smart.player.player.streamtv.live.watch.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel

object Common {
    var countInterAdd = 0
    var countInterSelect = 0
    var countInterBackPLay = 0
    var countInterItemPlaylist = 0
    var countInterAddOption = 0
    var isCheckChannel = false
    var titlte = ""

    private const val PREFS_NAME = "ChannelPrefs"
    private const val KEY_CHANNELS = "channels"
    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }
     fun checkAndroid13(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1000
                )
            }
        }
    }

     fun checkBoolAndroid13(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
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