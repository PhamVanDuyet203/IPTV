package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.db.PlaylistEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class ActivityImportPlaylistUrl : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var etPlaylistName: EditText
    private lateinit var etPlaylistUrl: EditText
    private lateinit var btnAddPlaylist: TextView
    private  lateinit var tvTitle: TextView

    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.import_playlist_url)

        btnBack = findViewById(R.id.btnBack)
        etPlaylistName = findViewById(R.id.etPlaylistName)
        etPlaylistUrl = findViewById(R.id.etPlaylistUrl)
        btnAddPlaylist = findViewById(R.id.btn_add_playlist)

        tvTitle = findViewById(R.id.tvTitle)
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }
        btnAddPlaylist.setOnClickListener { savePlaylist() }
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
                setResult(RESULT_OK)
                finish()
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