package com.samyak2403.iptvmine

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.adapter.GroupAdapter
import com.samyak2403.iptvmine.db.PlaylistEntity
import com.samyak2403.iptvmine.util.parseM3U
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

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = GroupAdapter(emptyList())
        recyclerView.adapter = adapter

        val playlistName = intent.getStringExtra("GROUP_NAME") ?: "Unknown Playlist"
        val sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""

        tvTitle.text = playlistName
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
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val channels = parseM3U(sourcePath)
                val groupedChannels = channels.groupBy { it.groupTitle ?: "Unknown" }
                val playlistEntities = groupedChannels.map { (groupTitle, channelList) ->
                    PlaylistEntity(
                        id = 0,
                        name = groupTitle,
                        channelCount = channelList.size,
                        sourceType = "URL",
                        sourcePath = sourcePath
                    )
                }.sortedBy { it.name }

                fullGroupList = playlistEntities
                adapter.updateData(playlistEntities)
            } catch (e: Exception) {
                Toast.makeText(this@ChannelListActivity, "Lỗi khi tải dữ liệu: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun filterGroups(query: String) {
        val filteredList = if (query.isEmpty()) {
            fullGroupList
        } else {
            fullGroupList.filter { it.name.contains(query, ignoreCase = true) }
        }
        adapter.updateData(filteredList)
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            searchEditText.visibility = View.GONE
            searchEditText.text.clear()
            filterGroups("")
        } else {
            searchEditText.visibility = View.VISIBLE
            searchEditText.requestFocus()
        }
        isSearchVisible = !isSearchVisible
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

        // Cập nhật màu nền dựa trên chế độ hiện tại
        sortAz.setBackgroundColor(if (currentSortMode == "AZ") highlightColor else defaultColor)
        sortZa.setBackgroundColor(if (currentSortMode == "ZA") highlightColor else defaultColor)
        sort09.setBackgroundColor(if (currentSortMode == "09") highlightColor else defaultColor)
        sort90.setBackgroundColor(if (currentSortMode == "90") highlightColor else defaultColor)

        sortAz.setOnClickListener {
            currentSortMode = "AZ"
            sortGroups { list -> list.sortedBy { it.name } }
            popupWindow.dismiss()
        }

        sortZa.setOnClickListener {
            currentSortMode = "ZA"
            sortGroups { list -> list.sortedByDescending { it.name } }
            popupWindow.dismiss()
        }

        sort09.setOnClickListener {
            currentSortMode = "09"
            sortGroups { list -> list.sortedBy { it.channelCount } }
            popupWindow.dismiss()
        }

        sort90.setOnClickListener {
            currentSortMode = "90"
            sortGroups { list -> list.sortedByDescending { it.channelCount } }
            popupWindow.dismiss()
        }
    }

    private fun sortGroups(sortFunction: (List<PlaylistEntity>) -> List<PlaylistEntity>) {
        val currentQuery = searchEditText.text.toString()
        val baseList = if (currentQuery.isEmpty()) fullGroupList else adapter.getGroups() // Sửa để dùng getGroups()
        val sortedList = sortFunction(baseList)
        adapter.updateData(sortedList)
    }
}