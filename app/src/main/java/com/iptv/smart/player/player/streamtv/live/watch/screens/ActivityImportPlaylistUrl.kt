package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SAVE_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistDeviceBinding
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistUrlBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ActivityImportPlaylistUrl : BaseActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var etPlaylistUrl: EditText
    private lateinit var btnAddPlaylist: TextView
    private  lateinit var tvTitle: TextView

    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }
    private val binding by lazy { ImportPlaylistUrlBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etPlaylistUrl = findViewById(R.id.etPlaylistUrl)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }
        btnAddPlaylist.setOnClickListener { savePlaylist() }

        showNativeAd()
    }

    private fun showNativeAd() {
        if (RemoteConfig.NATIVE_ADD_050325 == "1") {
            AdsManager.loadAndShowAdsNative(this, binding.frNative, AdsManager.NATIVE_ADD)
        }
        else binding.frNative.gone()
    }


    private fun savePlaylist() {
        val name = etPlaylistName.text.toString().trim()
        val url = etPlaylistUrl.text.toString().trim()

        if (name.isEmpty() || url.isEmpty()) {
            etPlaylistUrl.error = "Please enter a valid URL"
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val channelCount = countChannelsFromUrl(url)
            if (channelCount == 0) {
                withContext(Dispatchers.Main) {
                    etPlaylistUrl.error = "Invalid URL or no channels found"
                }
                return@launch
            }
            val playlist = PlaylistEntity(
                name = name,
                channelCount = channelCount,
                sourceType = "URL",
                sourcePath = url
            )
            playlistDao.insertPlaylist(playlist)
            withContext(Dispatchers.Main) {
                startAds()
            }
        }
    }

    private fun startAds() {
        when (RemoteConfig.INTER_SAVE_ADD_050325) {
            "0" -> {
                finish()
            }
            else -> {
                Common.countInterAdd++
                if (Common.countInterAdd % RemoteConfig.INTER_SAVE_ADD_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_SAVE_ADD) {
                        finish()
                    }
                } else {
                    finish()
                }
            }
        }
    }



    private suspend fun countChannelsFromUrl(urlString: String): Int {
        return withContext(Dispatchers.IO) {
            var count = 0
            try {
                val url = URL(urlString)
                val connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.requestMethod = "GET"

                connection.inputStream.bufferedReader().use { reader ->
                    reader.forEachLine { line ->
                        if (!line.startsWith("#") && line.isNotBlank()) {
                            count++
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            count
        }
    }
}