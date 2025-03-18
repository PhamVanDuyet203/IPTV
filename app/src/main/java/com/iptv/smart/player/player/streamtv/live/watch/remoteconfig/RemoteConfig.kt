package com.iptv.smart.player.player.streamtv.live.watch.remoteconfig

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.firebase.remoteconfig.ConfigUpdate
import com.google.firebase.remoteconfig.ConfigUpdateListener
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigException
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.iptv.smart.player.player.streamtv.live.watch.R

object RemoteConfig {

    var ADS_SPLASH_050325="1"
    var BANNER_SPLASH_050325="0"
    var NATIVE_LANGUAGE_050325="1"
    var INTER_LANGUAGE_050325="0"
    var NATIVE_INTRO_050325="0"
    var NATIVE_FULL_SCREEN_INTRO_050325="0"
    var ADS_HOME_050325="1"
    var INTER_ADD_050325="0"
    var NATIVE_ADD_050325="0"
    var INTER_SAVE_ADD_050325="1"
    var INTER_ITEMS_PLAYLIST_050325="0"
    var INTER_SELECT_CATEG_OR_CHANNEL_050325="0"
    var NATIVE_PLAYLIST_CHANNEL_050325="0"
    var BANNER_DETAIL_PLAYLIST_CHANNEL_050325="0"
    var INTER_BACK_PLAY_TO_LIST_050325="0"
    var ADS_PLAY_CONTROL_050325="1"
    var ONRESUME_050325="0"

    fun initRemoteConfig(completeListener: CompleteListener) {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings: FirebaseRemoteConfigSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(0)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.remote_config_defaults)

        mFirebaseRemoteConfig.addOnConfigUpdateListener(object : ConfigUpdateListener {
            override fun onUpdate(configUpdate: ConfigUpdate) {
                mFirebaseRemoteConfig.activate().addOnCompleteListener {
                    completeListener.onComplete()
                }
            }

            override fun onError(error: FirebaseRemoteConfigException) {
                Log.d("==check==", "onError: $error")
            }
        })

        mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener {
            Handler(Looper.getMainLooper()).postDelayed({
                completeListener.onComplete()
            }, 2000)
        }
    }

    @JvmStatic
    fun setReload(context: Context, open: Boolean) {
        val preferences =
            context.getSharedPreferences(context.packageName, Context.MODE_MULTI_PROCESS)
        preferences.edit().putBoolean("Reload", open).apply()
    }

    @JvmStatic
    fun getReload(mContext: Context): Boolean {
        val preferences =
            mContext.getSharedPreferences(mContext.packageName, Context.MODE_MULTI_PROCESS)
        return preferences.getBoolean("Reload", false)
    }

    interface CompleteListener {
        fun onComplete()
    }

    fun getValueAbTest(key: String): String {
        val mFirebaseRemoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getString(key)
    }
}