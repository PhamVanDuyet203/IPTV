package com.samyak2403.iptvmine.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.db.PlaylistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class ActivityImportPlaylistM3U : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var lnUpload: LinearLayout
    private lateinit var fileUploadedLayout: LinearLayout
    private lateinit var tvFileName: TextView
    private lateinit var btnRemoveFile: ImageView
    private lateinit var btnAddPlaylist: TextView

    private var selectedFileUri: Uri? = null
    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_playlist_m3u)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        lnUpload = findViewById(R.id.ln_upload)
        fileUploadedLayout = findViewById(R.id.fileUploadedLayout)
        tvFileName = findViewById(R.id.tvFileName)
        btnRemoveFile = findViewById(R.id.btnRemoveFile)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnRemoveFile.setOnClickListener { removeSelectedFile() }
        btnAddPlaylist.setOnClickListener { savePlaylist() }
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedFileUri = uri
                tvFileName.text = getFileName(uri)
                lnUpload.visibility = View.GONE
                fileUploadedLayout.visibility = View.VISIBLE
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
    }

    private fun removeSelectedFile() {
        selectedFileUri = null
        lnUpload.visibility = View.VISIBLE
        fileUploadedLayout.visibility = View.GONE
    }

    private fun savePlaylist() {
        val name = etPlaylistName.text.toString().trim()
        if (name.isEmpty() || selectedFileUri == null) {
            etPlaylistName.error = "Please enter a name and select a file"
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            val channelCount = countChannelsInM3U(selectedFileUri!!)
            val playlist = PlaylistEntity(
                name = name,
                channelCount = channelCount,
                sourceType = "FILE",
                sourcePath = selectedFileUri.toString()
            )
            playlistDao.insertPlaylist(playlist)
            setResult(RESULT_OK)
            finish()
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
                e.printStackTrace()
            }
            count
        }
    }

    private fun getFileName(uri: Uri): String {
        return uri.lastPathSegment ?: "Unknown.m3u"
    }
}