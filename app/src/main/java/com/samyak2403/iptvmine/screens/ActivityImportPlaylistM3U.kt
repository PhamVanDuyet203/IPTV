package com.samyak2403.iptvmine.screens

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
import com.samyak2403.iptvmine.ChannelListActivity
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
    private  lateinit var tvTitle: TextView

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

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnRemoveFile.setOnClickListener { removeSelectedFile() }
        btnAddPlaylist.setOnClickListener { savePlaylist() }
    }

    private val filePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                try {
                    // Yêu cầu quyền truy cập lâu dài
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    // Kiểm tra xem quyền có được cấp thành công không
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
                } catch (e: SecurityException) {
                    Toast.makeText(this,
                        getString(R.string.cannot_retain_file_access_permission_please_try_again), Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Log.w("M3UFile", "File selection canceled or failed with result code: ${result.resultCode}")
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
            etPlaylistName.error = getString(R.string.please_enter_a_name_and_select_a_file)
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channelCount = countChannelsInM3U(selectedFileUri!!)
                val playlist = PlaylistEntity(
                    name = name,
                    channelCount = channelCount,
                    sourceType = "FILE",
                    sourcePath = selectedFileUri.toString()
                )
                playlistDao.insertPlaylist(playlist)

                withContext(Dispatchers.Main) {
                    setResult(RESULT_OK)
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ActivityImportPlaylistM3U,
                        getString(R.string.error_saving_playlist, e.message), Toast.LENGTH_LONG).show()
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
        val fileName = uri.lastPathSegment ?: "Unknown.m3u"
        return fileName
    }
}