package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.admob.max.dktlibrary.AppOpenManager
import com.iptv.smart.player.player.streamtv.live.watch.ChannelListActivity
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SAVE_ADD
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistDeviceBinding
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ImportPlaylistM3uBinding
import com.iptv.smart.player.player.streamtv.live.watch.db.AppDatabase
import com.iptv.smart.player.player.streamtv.live.watch.db.PlaylistEntity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ActivityImportPlaylistM3U : BaseActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var lnUpload: LinearLayout
    private lateinit var fileUploadedLayout: LinearLayout
    private lateinit var tvFileName: TextView
    private lateinit var btnRemoveFile: ImageView
    private lateinit var btnAddPlaylist: TextView
    private lateinit var tvTitle: TextView

    private var selectedFileUri: Uri? = null
    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }
    private val binding by lazy { ImportPlaylistM3uBinding.inflate(layoutInflater) }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        lnUpload = findViewById(R.id.ln_upload)
        fileUploadedLayout = findViewById(R.id.fileUploadedLayout)
        tvFileName = findViewById(R.id.tvFileName)
        btnRemoveFile = findViewById(R.id.btnRemoveFile)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnRemoveFile.setOnClickListener { removeSelectedFile() }
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


    private val filePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                result.data?.data?.let { uri ->
                    try {
                        contentResolver.takePersistableUriPermission(
                            uri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                        val hasPermission = contentResolver.persistedUriPermissions.any {
                            it.uri == uri && it.isReadPermission
                        }

                        if (!hasPermission) {
                            return@let
                        }


                        selectedFileUri = uri
                        tvFileName.text = getFileName(uri)
                        lnUpload.visibility = View.GONE
                        fileUploadedLayout.visibility = View.VISIBLE
                        binding.errorTextFile.visibility = View.GONE
                    } catch (e: SecurityException) {
                        Toast.makeText(
                            this,
                            getString(R.string.cannot_retain_file_access_permission_please_try_again),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }


    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("audio/x-mpegurl", "application/x-mpegURL"))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePicker.launch(intent)
        AppOpenManager.getInstance().disableAppResumeWithActivity(ActivityImportPlaylistM3U::class.java)
    }

    override fun onResume() {
        super.onResume()
        AppOpenManager.getInstance().enableAppResumeWithActivity(ActivityImportPlaylistM3U::class.java)
    }

    private fun removeSelectedFile() {
        selectedFileUri = null
        lnUpload.visibility = View.VISIBLE
        fileUploadedLayout.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()
        showNativeAd()
    }

    private var isSaving = false
    private var lastSaveTime = 0L
    private val debounceDuration = 2000L

    private fun savePlaylist() {
        val name = etPlaylistName.text.toString().trim()
        if (name.isEmpty()) {
            binding.errorTextName.visibility = View.VISIBLE
            return
        } else if (selectedFileUri == null) {
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
                                getString(R.string.playlist_name_already_exists_file)
                            binding.btnAddPlaylist.isEnabled = true
                            isSaving = false
                        }
                        return@launch
                    }

                    val channelCount = countChannelsInM3U(selectedFileUri!!)
                    val playlist = PlaylistEntity(
                        name = name,
                        channelCount = channelCount,
                        sourceType = "FILE",
                        sourcePath = selectedFileUri.toString()
                    )
                    playlistDao.insertPlaylist(playlist)
                    withContext(Dispatchers.Main) {
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

    private suspend fun countChannelsInM3U(uri: Uri): Int {
        return withContext(Dispatchers.IO) {
            var count = 0
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        reader.forEachLine { line ->
                            if (!line.startsWith("#") && line.isNotBlank()) {
                                count++
                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
            count
        }
    }

    private fun getFileName(uri: Uri): String {
        var fileName = "Unknown.m3u"

        try {
            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex =
                        cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("M3UFile", "Error retrieving file name: ${e.message}")
        }

        if (!fileName.contains(".")) {
            fileName += ".m3u"
        }

        return fileName
    }
}