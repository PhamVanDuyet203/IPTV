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
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.adapter.ChannelsAdapter
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider
import com.samyak2403.iptvmine.screens.PlayerActivity
import com.samyak2403.iptvmine.util.parseM3U
import com.samyak2403.iptvmine.util.parseM3UFromFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelDetailActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelsAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false
    private var currentSortMode = "AZ"
    private lateinit var channelsProvider: ChannelsProvider
    private var groupName: String = "Unknown"
    private var currentQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_playlist_detail)

        recyclerView = findViewById(R.id.recyclerView)
        tvTitle = findViewById(R.id.tvTitle)
        btnBack = findViewById(R.id.btnBack)
        searchEditText = findViewById(R.id.searchEditText)
        searchIcon = findViewById(R.id.search_icon)
        sortIcon = findViewById(R.id.pop_sort)
        progressBar = findViewById(R.id.progressBar)

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        channelsProvider.init(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChannelsAdapter(
            channels = mutableListOf(),
            onChannelClicked = { channel ->
                PlayerActivity.start(this, channel)
                channelsProvider.addToRecent(channel)
            },
            onFavoriteClicked = { channel -> toggleFavorite(channel) },
            onRenameChannel = { channel, newName ->
                channelsProvider.updateChannel(channel.copy(name = newName))
            },
            onDeleteChannel = { channel ->
                channelsProvider.deleteChannel(channel)
            }
        )
        recyclerView.adapter = adapter

        groupName = intent.getStringExtra("GROUP_NAME") ?: "Unknown"
        val sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""

        Log.d("ChannelDetailActivity", "Received groupName: $groupName, sourcePath: $sourcePath")

        tvTitle.text = groupName
        tvTitle.isSelected = true

        btnBack.setOnClickListener { finish() }

        searchIcon.setOnClickListener { toggleSearchBar() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceHandler?.removeCallbacksAndMessages(null)
                debounceHandler = Handler(Looper.getMainLooper())
                debounceHandler?.postDelayed({
                    currentQuery = s.toString()
                    filterChannels(currentQuery)
                }, 500)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sortIcon.setOnClickListener { showSortPopup(it) }

        loadChannels(sourcePath)
        observeChannels()
    }

    private fun loadChannels(sourcePath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE // Hiển thị ProgressBar trên luồng Main
            try {
                val channels = withContext(Dispatchers.IO) { // Chuyển tải dữ liệu sang luồng IO
                    if (sourcePath.startsWith("http://") || sourcePath.startsWith("https://")) {
                        Log.d("ChannelDetailActivity", "Loading from URL: $sourcePath")
                        parseM3U(sourcePath)
                    } else {
                        Log.d("ChannelDetailActivity", "Loading from file: $sourcePath")
                        val uri = android.net.Uri.parse(sourcePath)
                        val hasPermission = contentResolver.persistedUriPermissions.any {
                            it.uri == uri && it.isReadPermission
                        }
                        if (!hasPermission) {
                            throw Exception("Không có quyền truy cập tệp. Vui lòng chọn lại.")
                        }
                        val inputStream = contentResolver.openInputStream(uri)
                        inputStream?.use { parseM3UFromFile(it) } ?: throw Exception("Không thể mở tệp")
                    }
                }

                val filteredChannels = channels.filter { it.groupTitle == groupName }
                    .map { channel ->
                        Channel(
                            name = channel.name,
                            streamUrl = channel.streamUrl,
                            logoUrl = channel.logoUrl ?: "",
                            isFavorite = channelsProvider.isFavorite(channel.streamUrl),
                            groupTitle = channel.groupTitle
                        )
                    }

                Log.d("ChannelDetailActivity", "Loaded ${channels.size} channels, filtered to ${filteredChannels.size} for group $groupName")

                channelsProvider.addChannelsFromM3U(filteredChannels)
                if (filteredChannels.isEmpty()) {
                    Toast.makeText(this@ChannelDetailActivity, "Không có kênh nào trong nhóm $groupName", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ChannelDetailActivity, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
                Log.e("ChannelDetailActivity", "Error loading channels: ${e.message}", e)
            } finally {
                progressBar.visibility = View.GONE // Ẩn ProgressBar trong mọi trường hợp (thành công hoặc lỗi)
            }
        }
    }

    private fun filterChannels(query: String) {
        channelsProvider.channels.value?.let { allChannels ->
            val filteredList = allChannels.filter { it.groupTitle == groupName }
                .let { list ->
                    if (query.isEmpty()) list else list.filter { it.name.contains(query, ignoreCase = true) }
                }
            val sortedList = when (currentSortMode) {
                "AZ" -> filteredList.sortedBy { it.name }
                "ZA" -> filteredList.sortedByDescending { it.name }
                else -> filteredList
            }
            Log.d("ChannelDetailActivity", "Filtered ${sortedList.size} channels with query: $query")
            adapter.updateChannels(sortedList)
        }
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            searchEditText.visibility = View.GONE
            searchEditText.text.clear()
            currentQuery = ""
            filterChannels("")
        } else {
            searchEditText.visibility = View.VISIBLE
            searchEditText.requestFocus()
        }
        isSearchVisible = !isSearchVisible
    }

    private fun showSortPopup(anchorView: View) {
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
            filterChannels(searchEditText.text.toString())
            popupWindow.dismiss()
        }

        sortZa.setOnClickListener {
            currentSortMode = "ZA"
            filterChannels(searchEditText.text.toString())
            popupWindow.dismiss()
        }
    }

    private fun toggleFavorite(channel: Channel) {
        channelsProvider.toggleFavorite(channel)
        filterChannels(currentQuery)
    }

    private fun observeChannels() {
        channelsProvider.channels.observe(this) { channels ->
            val filteredList = channels.filter { it.groupTitle == groupName }
            val sortedList = when (currentSortMode) {
                "AZ" -> filteredList.sortedBy { it.name }
                "ZA" -> filteredList.sortedByDescending { it.name }
                else -> filteredList
            }
            Log.d("ChannelDetailActivity", "Displaying ${sortedList.size} channels for group $groupName")
            adapter.updateChannels(sortedList)
        }
    }
}