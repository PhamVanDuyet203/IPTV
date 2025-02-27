package com.samyak2403.iptvmine

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.adapter.GroupAdapter
import com.samyak2403.iptvmine.db.PlaylistEntity
import com.samyak2403.iptvmine.util.parseM3U
import com.samyak2403.iptvmine.util.parseM3UFromFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChannelListActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: GroupAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar // Thêm ProgressBar
    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false
    private var fullGroupList: List<PlaylistEntity> = emptyList()
    private var currentSortMode = "AZ"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        recyclerView = findViewById(R.id.recyclerView)
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.search_icon)
        sortIcon = findViewById(R.id.pop_sort)
        progressBar = findViewById(R.id.progressBar) // Khởi tạo ProgressBar

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GroupAdapter(emptyList())
        recyclerView.adapter = adapter

        val playlistName = intent.getStringExtra("GROUP_NAME") ?: "Unknown Playlist"
        val sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""

        Log.d("ChannelListActivity", "Received source file path: $sourcePath")
        Log.d("ChannelListActivity", "Playlist name: $playlistName")

        tvTitle.text = playlistName
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }

        searchIcon.setOnClickListener { toggleSearchBar() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceHandler?.removeCallbacksAndMessages(null)
                debounceHandler = Handler(Looper.getMainLooper())
                debounceHandler?.postDelayed({
                    filterGroups(s.toString())
                }, 500)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sortIcon.setOnClickListener { showSortPopup(it) }

        loadGroupedChannels(sourcePath)
    }

    private fun loadGroupedChannels(sourcePath: String) {
        Log.d("ChannelListActivity", "Loading channels from sourcePath: $sourcePath")
        CoroutineScope(Dispatchers.Main).launch {
            try {
                progressBar.visibility = View.VISIBLE // Hiển thị ProgressBar khi bắt đầu tải
                val channels = if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
                    Log.d("ChannelListActivity", "Parsing M3U from URL: $sourcePath")
                    parseM3U(sourcePath)
                } else {
                    val uri = android.net.Uri.parse(sourcePath)
                    Log.d("ChannelListActivity", "Parsed URI from sourcePath: $uri")

                    val hasPermission = contentResolver.persistedUriPermissions.any {
                        it.uri == uri && it.isReadPermission
                    }
                    Log.d("ChannelListActivity", "Checking permissions for URI: $uri")
                    Log.d("ChannelListActivity", "Persisted permissions: ${contentResolver.persistedUriPermissions}")
                    Log.d("ChannelListActivity", "Has read permission: $hasPermission")

                    if (!hasPermission) {
                        Log.w("ChannelListActivity", "No read permission for URI: $uri")
                        Toast.makeText(this@ChannelListActivity, "Quyền truy cập tệp đã bị thu hồi. Vui lòng chọn lại tệp.", Toast.LENGTH_LONG).show()
                        finish()
                        return@launch
                    }

                    val inputStream = try {
                        contentResolver.openInputStream(uri)
                            ?: throw Exception("Failed to open input stream for URI: $uri")
                    } catch (e: SecurityException) {
                        throw Exception("Permission denied for URI: $uri. Please select the file again.")
                    }
                    Log.d("ChannelListActivity", "Successfully opened input stream for URI: $uri")
                    inputStream.use { parseM3UFromFile(it) }
                }
                val groupedChannels = channels.groupBy { it.groupTitle ?: "Unknown" }
                val playlistEntities = groupedChannels.map { (groupTitle, channelList) ->
                    PlaylistEntity(
                        id = 0,
                        name = groupTitle,
                        channelCount = channelList.size,
                        sourceType = if (sourcePath.startsWith("http")) "URL" else "FILE",
                        sourcePath = sourcePath
                    )
                }.sortedBy { it.name }

                fullGroupList = playlistEntities
                adapter.updateData(playlistEntities)
                Log.d("ChannelListActivity", "Loaded ${playlistEntities.size} playlist entities from source file")
                progressBar.visibility = View.GONE // Ẩn ProgressBar khi dữ liệu tải xong
            } catch (e: Exception) {
                Toast.makeText(this@ChannelListActivity, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ChannelListActivity", "Error loading channels: ${e.message}", e)
                progressBar.visibility = View.GONE // Ẩn ProgressBar khi có lỗi
            }
        }
    }

    private fun filterGroups(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullGroupList
        } else {
            fullGroupList.filter { it.name.contains(query, ignoreCase = true) }
        }
        val sortedList = when (currentSortMode) {
            "AZ" -> filteredList.sortedBy { it.name }
            "ZA" -> filteredList.sortedByDescending { it.name }
            "09" -> filteredList.sortedBy { it.channelCount }
            "90" -> filteredList.sortedByDescending { it.channelCount }
            else -> filteredList
        }
        adapter.updateData(sortedList)
    }

    private fun toggleSearchBar() {
        // Giữ nguyên logic của bạn
    }

    private fun showSortPopup(anchorView: View) {
        val inflater = LayoutInflater.from(this)
        val popupView = inflater.inflate(R.layout.popup_sorting, null)

        val popupWindow = PopupWindow(
            popupView,
            (156 * resources.displayMetrics.density).toInt(),
            (235 * resources.displayMetrics.density).toInt(),
            true
        )

        val marginTopPx = (8 * resources.displayMetrics.density).toInt()
        popupWindow.showAsDropDown(anchorView, 0, marginTopPx, Gravity.END)

        val sortAz = popupView.findViewById<TextView>(R.id.sort_az)
        val sortZa = popupView.findViewById<TextView>(R.id.sort_za)
        val sort09 = popupView.findViewById<TextView>(R.id.sort_09)
        val sort90 = popupView.findViewById<TextView>(R.id.sort_90)

        val defaultColor = android.graphics.Color.TRANSPARENT
        val highlightColor = android.graphics.Color.parseColor("#D0E4FF")

        sortAz.setBackgroundColor(if (currentSortMode == "AZ") highlightColor else defaultColor)
        sortZa.setBackgroundColor(if (currentSortMode == "ZA") highlightColor else defaultColor)
        sort09.setBackgroundColor(if (currentSortMode == "09") highlightColor else defaultColor)
        sort90.setBackgroundColor(if (currentSortMode == "90") highlightColor else defaultColor)

        sortAz.setOnClickListener {
            currentSortMode = "AZ"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sortZa.setOnClickListener {
            currentSortMode = "ZA"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sort09.setOnClickListener {
            currentSortMode = "09"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sort90.setOnClickListener {
            currentSortMode = "90"
            filterGroups(searchEditText.text.toString())
            popupWindow.dismiss()
        }
    }
}