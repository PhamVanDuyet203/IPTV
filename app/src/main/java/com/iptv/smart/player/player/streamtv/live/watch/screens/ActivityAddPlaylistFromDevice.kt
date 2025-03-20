package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admob.max.dktlibrary.AppOpenManager
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.VideoAdapter
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SAVE_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistDeviceBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
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
    private val videoList = mutableListOf<Channel>()
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

        if (binding.etPlaylistName.getText().toString().isEmpty()) {
            binding.btnAddPlaylist.visibility = View.GONE
        } else {
            binding.btnAddPlaylist.visibility = View.VISIBLE
        }

        rvVideo.adapter = videoAdapter

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnAddPlaylist.setOnClickListener {
            savePlaylist()
        }

        binding.etPlaylistName.addTextChangedListener() {
            binding.btnAddPlaylist.visibility = View.VISIBLE
            binding.errorTextName.visibility = View.GONE
        }


    }

    private fun showNativeAd() {
        if (RemoteConfig.NATIVE_ADD_050325 == "1") {
            AdsManager.loadAndShowAdsNative(this, binding.frNative, AdsManager.NATIVE_ADD)
        } else binding.frNative.gone()
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
                    val fileSize = getFileSizeFromFileUri(uri) ?: return@let
                    val maxSize = 30 * 1024 * 1024
                    if (fileName != null && fileSize <= maxSize) {
                        videoList.add(Channel(fileName, "", uri.toString()))
                        videoAdapter.notifyDataSetChanged()
                    } else {
                        Toast.makeText(
                            this,
                            getString(R.string.error_file_size),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    fun getFileSizeFromFileUri(uri: Uri): Long? {
        return try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                    if (sizeIndex != -1) cursor.getLong(sizeIndex) else null
                } else {
                    null
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        AppOpenManager.getInstance()
            .enableAppResumeWithActivity(ActivityAddPlaylistFromDevice::class.java)
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "video/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        videoPicker.launch(intent)
        AppOpenManager.getInstance()
            .disableAppResumeWithActivity(ActivityAddPlaylistFromDevice::class.java)
    }

    private fun removeVideo(videoItem: Channel) {
        videoList.remove(videoItem)
        videoAdapter.notifyDataSetChanged()
    }

    override fun onStart() {
        super.onStart()
        showNativeAd()
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
            binding.progressBar.visible()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val existingPlaylist = playlistDao.getPlaylistByName(name)
                    if (existingPlaylist != null) {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.gone()
                            etPlaylistName.error =
                                getString(R.string.playlist_name_already_exists_video)
                            binding.btnAddPlaylist.isEnabled = true
                            isSaving = false
                        }
                        return@launch
                    }
                    val playlist = PlaylistEntity(
                        name = name,
                        channelCount = videoList.size,
                        sourceType = "DEVICE",
                        sourcePath = videoList.joinToString(";") { it.streamUrl.toString() }
                    )
                    playlistDao.insertPlaylist(playlist)

                    withContext(Dispatchers.Main) {
                        startAds()
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@ActivityAddPlaylistFromDevice,
                            getString(R.string.error_saving_playlist_video), Toast.LENGTH_SHORT
                        ).show()
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
                binding.progressBar.gone()
                setResult(RESULT_OK)
                finish()
            }

            else -> {
                Log.d("TAG121212212", "startAds: " + Common.countInterAdd)
                Log.d(
                    "TAG121212212",
                    "startAdsINTER_SAVE_ADD_050325: " + RemoteConfig.INTER_SAVE_ADD_050325
                )
                Common.countInterAdd++
                if (Common.countInterAdd % RemoteConfig.INTER_SAVE_ADD_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_SAVE_ADD) {
                        setResult(RESULT_OK)
                        finish()
                        binding.progressBar.gone()
                    }
                } else {
                    binding.progressBar.gone()
                    setResult(RESULT_OK)
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