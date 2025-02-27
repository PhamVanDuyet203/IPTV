package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.VideoDetailAdapter
import com.samyak2403.iptvmine.db.AppDatabase
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.model.VideoItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoDetailAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private val playlistDao by lazy { AppDatabase.getDatabase(this).playlistDao() }
    private var videoListFull = mutableListOf<VideoItem>()
    private var isSearchVisible = false
    private var currentSortMode = "AZ"

    private var groupName: String = "Unknown"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        recyclerView = findViewById(R.id.recyclerView)
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.search_icon)
        sortIcon = findViewById(R.id.pop_sort)
        progressBar = findViewById(R.id.progressBar) // Thêm ProgressBar

        // Hiển thị ProgressBar khi bắt đầu tải
        showLoading()

        // Lấy dữ liệu từ Intent
        val groupName = intent.getStringExtra("GROUP_NAME") ?: "Unknown"
        val sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""
        Log.d("VideoDetailActivity", "onCreate: GROUP_NAME=$groupName, SOURCE_PATH=$sourcePath")

        // Thiết lập tiêu đề và nút back
        tvTitle.text = groupName
        tvTitle.isSelected = true
        btnBack.setOnClickListener { finish() }

        // Tải dữ liệu trong Coroutine để không chặn UI thread
        CoroutineScope(Dispatchers.Main).launch {
            loadVideoData(sourcePath, groupName)
            hideLoading() // Ẩn ProgressBar sau khi tải xong
        }

        // Xử lý nút tìm kiếm
        searchIcon.setOnClickListener { toggleSearchBar() }

        // Xử lý tìm kiếm
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVideos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Xử lý nút sắp xếp
        sortIcon.setOnClickListener { showSortPopup(it) }
    }

    private suspend fun loadVideoData(sourcePath: String, groupName: String) {
        // Chuẩn bị danh sách video từ sourcePath
        videoListFull = sourcePath.split(";")
            .filter { it.isNotEmpty() }
            .map { uriString ->
                val uri = android.net.Uri.parse(uriString)
                VideoItem(
                    uri = uri,
                    fileName = getFileName(uriString) ?: "Unnamed Video",
                    groupTitle = groupName
                )
            }.toMutableList()

        // Thiết lập RecyclerView với VideoDetailAdapter
        adapter = VideoDetailAdapter(
            context = this,
            videoList = videoListFull,
            onPlayClicked = { videoItem ->
                val channel = Channel(
                    name = videoItem.fileName,
                    streamUrl = videoItem.uri.toString(),
                    logoUrl = "assets/images/ic_tv.png",
                    isFavorite = videoItem.isFavorite,
                    groupTitle = videoItem.groupTitle ?: groupName
                )
                PlayerActivity.start(this, channel)
            },
            onFavoriteClicked = { videoItem ->
                Log.d("VideoDetailActivity", "Favorite toggled for: ${videoItem.fileName}")
            },
            onRenameVideo = { videoItem, newName ->
                Log.d("VideoDetailActivity", "Renamed ${videoItem.fileName} to $newName")
                updatePlaylistInDatabase(groupName, videoListFull)
            },
            onDeleteVideo = { videoItem ->
                Log.d("VideoDetailActivity", "Deleted: ${videoItem.fileName}")
                updatePlaylistInDatabase(groupName, videoListFull)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }

    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
    }

    // Các hàm khác giữ nguyên
    private fun getFileName(uriString: String): String? {
        val uri = android.net.Uri.parse(uriString)
        return try {
            val cursor = contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val nameIndex = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    return if (nameIndex != -1) it.getString(nameIndex) else null
                }
            }
            Log.w("VideoDetailActivity", "getFileName: Failed to retrieve name for URI: $uriString")
            null
        } catch (e: Exception) {
            Log.e("VideoDetailActivity", "getFileName: Error accessing URI: $uriString, ${e.message}")
            null
        }
    }

    private fun updatePlaylistInDatabase(groupName: String, updatedVideoList: List<VideoItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            val playlist = playlistDao.getPlaylistByName(groupName)
            if (playlist != null) {
                val updatedSourcePath = updatedVideoList.joinToString(";") { it.uri.toString() }
                val updatedPlaylist = playlist.copy(
                    sourcePath = updatedSourcePath,
                    channelCount = updatedVideoList.size
                )
                playlistDao.updatePlaylist(updatedPlaylist)
                Log.d("VideoDetailActivity", "Updated playlist in DB: $updatedSourcePath")
            }
        }
    }

    private fun toggleSearchBar() {
        // Giữ nguyên code gốc
    }

    private fun filterVideos(query: String) {
        showLoading() // Hiển thị ProgressBar khi bắt đầu lọc
        val filteredList = videoListFull.filter { it.fileName.contains(query, ignoreCase = true) }
        val sortedList = when (currentSortMode) {
            "AZ" -> filteredList.sortedBy { it.fileName }
            "ZA" -> filteredList.sortedByDescending { it.fileName }
            else -> filteredList
        }
        adapter = VideoDetailAdapter(
            context = this,
            videoList = sortedList.toMutableList(),
            onPlayClicked = { videoItem ->
                val channel = Channel(
                    name = videoItem.fileName,
                    streamUrl = videoItem.uri.toString(),
                    logoUrl = "assets/images/ic_tv.png",
                    isFavorite = videoItem.isFavorite,
                    groupTitle = videoItem.groupTitle ?: sortedList.firstOrNull()?.groupTitle ?: "Unknown"
                )
                PlayerActivity.start(this, channel)
            },
            onFavoriteClicked = { videoItem ->
                Log.d("VideoDetailActivity", "Favorite toggled for: ${videoItem.fileName}")
            },
            onRenameVideo = { videoItem, newName ->
                Log.d("VideoDetailActivity", "Renamed ${videoItem.fileName} to $newName")
                updatePlaylistInDatabase(groupName, videoListFull)
            },
            onDeleteVideo = { videoItem ->
                Log.d("VideoDetailActivity", "Deleted: ${videoItem.fileName}")
                updatePlaylistInDatabase(groupName, videoListFull)
            }
        )
        recyclerView.adapter = adapter
        hideLoading() // Ẩn ProgressBar sau khi lọc xong
    }

    private fun showSortPopup(anchorView: View) {
        // Giữ nguyên code gốc
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_sorting_channel, null)

        val popupWindow = PopupWindow(
            popupView,
            (156 * resources.displayMetrics.density).toInt(),
            (130 * resources.displayMetrics.density).toInt(),
            true
        )

        val marginTopPx = (8 * resources.displayMetrics.density).toInt()
        popupWindow.showAsDropDown(anchorView, 0, marginTopPx, Gravity.END)

        val sortAz = popupView.findViewById<TextView>(R.id.sort_az)
        val sortZa = popupView.findViewById<TextView>(R.id.sort_za)
        val defaultColor = android.graphics.Color.TRANSPARENT
        val highlightColor = android.graphics.Color.parseColor("#D0E4FF")

        sortAz.setBackgroundColor(if (currentSortMode == "AZ") highlightColor else defaultColor)
        sortZa.setBackgroundColor(if (currentSortMode == "ZA") highlightColor else defaultColor)

        sortAz.setOnClickListener {
            currentSortMode = "AZ"
            filterVideos(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sortZa.setOnClickListener {
            currentSortMode = "ZA"
            filterVideos(searchEditText.text.toString())
            popupWindow.dismiss()
        }
    }
}