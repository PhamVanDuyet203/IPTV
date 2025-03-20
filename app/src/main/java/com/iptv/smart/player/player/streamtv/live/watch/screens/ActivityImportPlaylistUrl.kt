package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SAVE_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
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
    private lateinit var tvTitle: TextView

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
        btnAddPlaylist.setOnClickListener {
            savePlaylist()
        }

        binding.etPlaylistName.addTextChangedListener() {
            binding.errorTextName.visibility = View.GONE
            binding.btnAddPlaylist.visibility = View.VISIBLE
        }
        binding.etPlaylistUrl.addTextChangedListener() {
            binding.btnAddPlaylist.visibility = View.VISIBLE
            binding.errorTextURL.visibility = View.GONE
        }

    }

    private fun showNativeAd() {
        if (RemoteConfig.NATIVE_ADD_050325 == "1") {
            AdsManager.loadAndShowAdsNative(this, binding.frNative, AdsManager.NATIVE_ADD)
        } else {
            binding.frNative.gone()
        }
    }

    private var isSaving = false
    private var lastSaveTime = 0L
    private val debounceDuration = 2000L

    private fun savePlaylist() {
        val name = etPlaylistName.text.toString().trim()
        val url = etPlaylistUrl.text.toString().trim()

        if (url.isEmpty() && name.isEmpty()) {
            binding.errorTextName.visibility = View.VISIBLE
            binding.errorTextURL.visibility = View.VISIBLE
            return
        } else if (url.isEmpty()) {
            binding.errorTextURL.visibility = View.VISIBLE
            return
        } else if (name.isEmpty()) {
            binding.errorTextName.visibility = View.VISIBLE
            return
        } else {
            val currentTime = System.currentTimeMillis()
            if (isSaving || currentTime - lastSaveTime < debounceDuration) return

            showOverlay()

            isSaving = true
            lastSaveTime = currentTime
            binding.btnAddPlaylist.isEnabled = false
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val existingPlaylist = playlistDao.getPlaylistByName(name)
                    if (existingPlaylist != null) {
                        withContext(Dispatchers.Main) {
                            hideOverlay()
                            etPlaylistName.error = getString(R.string.playlist_name_already_exists)
                            binding.btnAddPlaylist.isEnabled = true
                            isSaving = false
                        }
                        return@launch
                    }
                    val channelCount = fetchAndCountChannelsFromUrl(url)
                    if (channelCount == 0) {
                        withContext(Dispatchers.Main) {
                            hideOverlay()
                            etPlaylistUrl.error =
                                getString(R.string.invalid_url_or_no_channels_found)
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
                        hideOverlay()
                        startAds()
                    }
                } finally {
                    isSaving = false
                    withContext(Dispatchers.Main) {
                        binding.btnAddPlaylist.isEnabled = true
                    }
                }
            }
        }


    }

    private fun showOverlay() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideOverlay() {
        binding.progressBar.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        showNativeAd()
    }

    private fun startAds() {
        when (RemoteConfig.INTER_SAVE_ADD_050325) {
            "0" -> {
                setResult(RESULT_OK)
                finish()
                binding.progressBar.gone()
            }

            else -> {
                Common.countInterAdd++
                if (Common.countInterAdd % RemoteConfig.INTER_SAVE_ADD_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_SAVE_ADD) {
                        setResult(RESULT_OK)
                        finish()
                        binding.progressBar.gone()
                    }
                } else {
                    setResult(RESULT_OK)
                    finish()
                    binding.progressBar.gone()
                }
            }
        }
    }

    private suspend fun fetchAndCountChannelsFromUrl(urlString: String): Int {
        return withContext(Dispatchers.IO) {
            var channelCount = 0
            try {
                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.connectTimeout = 20000
                connection.readTimeout = 20000
                connection.requestMethod = "GET"

                val fileText = connection.inputStream.bufferedReader().use { it.readText() }
                connection.disconnect()

                val lines = fileText.split("\n")
                var name: String? = null
                var streamUrl: String? = null

                for (line in lines) {
                    when {
                        line.startsWith("#EXTINF:") -> {
                            name = extractChannelName(line)
                        }

                        line.isNotEmpty() && !line.startsWith("#") -> {
                            streamUrl = line
                            if (!name.isNullOrEmpty() && streamUrl != null) {
                                channelCount++
                            }
                            name = null
                            streamUrl = null
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            channelCount
        }
    }

    private fun extractChannelName(line: String): String? = line.split(",").lastOrNull()?.trim()
}