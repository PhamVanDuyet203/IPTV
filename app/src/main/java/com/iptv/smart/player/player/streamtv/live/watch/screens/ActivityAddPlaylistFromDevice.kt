package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
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
            startAds()
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

    private fun savePlaylist() {
        val name = etPlaylistName.text.toString().trim()
        if (name.isEmpty() || videoList.isEmpty()) {
            etPlaylistName.error =
                getString(R.string.please_enter_a_name_and_select_at_least_one_video)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val playlist = PlaylistEntity(
                name = name,
                channelCount = videoList.size,
                sourceType = "DEVICE",
                sourcePath = videoList.joinToString(";") { it.uri.toString() }
            )
            playlistDao.insertPlaylist(playlist)

            val channelsProvider = ChannelsProvider()
            channelsProvider.init(this@ActivityAddPlaylistFromDevice)
            channelsProvider.refreshChannels()
            setResult(RESULT_OK)

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