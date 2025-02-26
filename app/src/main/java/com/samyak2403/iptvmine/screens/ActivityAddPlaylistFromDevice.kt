package com.samyak2403.iptvmine.screens

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
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.VideoAdapter
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.db.PlaylistEntity
import com.samyak2403.iptvmine.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ActivityAddPlaylistFromDevice : AppCompatActivity() {
    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var lnUpload: LinearLayout
    private lateinit var rvVideo: RecyclerView
    private lateinit var btnAddPlaylist: TextView
    private lateinit var videoAdapter: VideoAdapter
    private val videoList = mutableListOf<VideoItem>()
    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_playlist_device)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        lnUpload = findViewById(R.id.ln_upload)
        rvVideo = findViewById(R.id.rvVideo)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        videoAdapter = VideoAdapter(this, videoList) { videoItem -> removeVideo(videoItem) }
        rvVideo.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvVideo.adapter = videoAdapter

        btnBack.setOnClickListener { finish() }
        lnUpload.setOnClickListener { openFilePicker() }
        btnAddPlaylist.setOnClickListener { savePlaylist() }
    }

    private val videoPicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
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
            etPlaylistName.error = "Please enter a name and select at least one video"
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
            setResult(RESULT_OK)
            finish()
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