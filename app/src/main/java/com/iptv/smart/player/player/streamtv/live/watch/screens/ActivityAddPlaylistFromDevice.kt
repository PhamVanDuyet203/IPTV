package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.VideoAdapter
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SAVE_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistDeviceBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.dialog.ImportPlaylistDialog
import com.iptv.smart.player.player.streamtv.live.watch.model.VideoItem
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ActivityAddPlaylistFromDevice : BaseActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var lnUpload: LinearLayout
    private lateinit var rvVideo: RecyclerView
    private lateinit var btnAddPlaylist: TextView
    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<VideoItem>()
    private lateinit var tvTitle: TextView
    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }

    private val binding by lazy { ImportPlaylistDeviceBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        lnUpload = findViewById(R.id.ln_upload)
        rvVideo = findViewById(R.id.rvVideo)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.isSelected = true

        videoAdapter = VideoAdapter(this, videoList) { videoItem -> removeVideo(videoItem) }
        rvVideo.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvVideo.adapter = videoAdapter

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnAddPlaylist.setOnClickListener {
            savePlaylist()
        }

        binding.etPlaylistName.addTextChangedListener() {
            binding.errorTextName.visibility = View.GONE
        }

        showNativeAd()

    }

    private fun showNativeAd() {
        if (RemoteConfig.NATIVE_ADD_050325 == "1") {
            AdsManager.loadAndShowAdsNative(this, binding.frNative, AdsManager.NATIVE_ADD)
        }
        else binding.frNative.gone()
    }

    private val videoPicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    binding.errorTextFile.visibility = View.GONE
                    val fileName = getFileName(uri)
                    if (fileName != null) {
                        videoList.add(VideoItem(uri, fileName))
                        videoAdapter.notifyDataSetChanged()
                    }
                }
            }
        }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        videoPicker.launch(intent)
    }

    private fun removeVideo(videoItem: VideoItem) {
        videoList.remove(videoItem)
        videoAdapter.notifyDataSetChanged()
    }


    private var isSaving = false
    private var lastSaveTime = 0L
    private val debounceDuration = 2000L

    private fun savePlaylist() {
        val name = binding.etPlaylistName.text.toString().trim()
        if (name.isEmpty()) {
            binding.errorTextName.visibility = View.VISIBLE
            return
        } else if (videoList.isEmpty()) {
            binding.errorTextFile.visibility = View.VISIBLE
            return
        } else {
            val currentTime = System.currentTimeMillis()
            if (isSaving || currentTime - lastSaveTime < debounceDuration) return

            isSaving = true
            lastSaveTime = currentTime
            binding.btnAddPlaylist.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val existingPlaylist = playlistDao.getPlaylistByName(name)
                    if (existingPlaylist != null) {
                        withContext(Dispatchers.Main) {
                            etPlaylistName.error = "Playlist name already exists"
                            binding.btnAddPlaylist.isEnabled = true
                            isSaving = false
                        }
                        return@launch
                    }
                    val playlist = PlaylistEntity(
                        name = name,
                        channelCount = videoList.size,
                        sourceType = "DEVICE",
                        sourcePath = videoList.joinToString(";") { it.uri.toString() }
                    )
                    playlistDao.insertPlaylist(playlist)

                    withContext(Dispatchers.Main) {
                        startAds()
                        setResult(RESULT_OK)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SavePlaylist", "Error saving playlist: ${e.message}")
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ActivityAddPlaylistFromDevice, "Error saving playlist", Toast.LENGTH_SHORT).show()
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

    private fun getFileName(uri: Uri): String? {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                return if (nameIndex != -1) it.getString(nameIndex) else null
            }
        }
        return null
    }
}