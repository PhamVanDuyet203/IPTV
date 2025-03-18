package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.PopupWindow
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.VideoDetailAdapter
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class VideoDetailActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: VideoDetailAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private var videoListFull = mutableListOf<Channel>()
    private var currentSortMode = "AZ"
    private var groupName: String = "Unknown"
    private lateinit var imgNotFound: ImageView
    private lateinit var txtNotFound: TextView


    // new
    private val playerResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data
            val refreshData = data?.getBooleanExtra("REFRESH_DATA", false) ?: false
            val channelName = data?.getStringExtra("CHANNEL_NAME")
            val isFavorite = data?.getBooleanExtra("IS_FAVORITE", false) ?: false

            if (refreshData && channelName != null) {
                val channel = videoListFull.find { it.name == channelName }
                channel?.isFavorite = isFavorite
                adapter.notifyDataSetChanged()
            }
        }
    }

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

        imgNotFound = findViewById(R.id.imgNotFound)
        txtNotFound = findViewById(R.id.txtNotFound)
        imgNotFound.visibility = View.GONE
        txtNotFound.visibility = View.GONE

        adapter = VideoDetailAdapter(
            context = this,
            videoList = mutableListOf(),
            onPlayClicked = { videoItem ->
                val channel = Channel(
                    name = videoItem.name,
                    streamUrl = videoItem.run { streamUrl },
                    logoUrl = videoItem.streamUrl,
                    isFavorite = videoItem.isFavorite,
                    groupTitle = videoItem.groupTitle ?: groupName
                )

                // new
                val intent = Intent(this, PlayerActivity::class.java).apply {
                    putExtra("channel", channel)
                }
                playerResultLauncher.launch(intent)
            },
            onFavoriteClicked = { videoItem ->
                toggleFavorite(this,videoItem)
            },

        )
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        showLoading()

        groupName = intent.getStringExtra("GROUP_NAME") ?: "Unknown"
        val sourcePath = intent.getStringExtra("SOURCE_PATH") ?: ""
        Log.d("VideoDetailActivity", "onCreate: GROUP_NAME=$groupName, SOURCE_PATH=$sourcePath")

        tvTitle.text = groupName
        tvTitle.isSelected = true
        btnBack.setOnClickListener { finish() }

        CoroutineScope(Dispatchers.Main).launch {
            loadVideoData(sourcePath, groupName)
            hideLoading()
        }

        searchIcon.setOnClickListener { toggleSearchBar() }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterVideos(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(v.windowToken, 0)
                true
            } else {
                false
            }
        }

        sortIcon.setOnClickListener { showSortPopup(it) }
    }
    private fun toggleFavorite(context: Context, channel: Channel) {

        channel.isFavorite = !channel.isFavorite
        val checkChannel = Channel(
            name = channel.name,
            streamUrl = channel.streamUrl,
            logoUrl = channel.logoUrl,
            isFavorite = false,
            groupTitle = ""
        )

        val listFav: ArrayList<Channel> = ArrayList(Common.getChannels(context))
        if (listFav.contains(checkChannel)){
            listFav.remove(checkChannel)
        }else{
            listFav.add(checkChannel)
        }
        Common.saveChannels(context, listFav)
        adapter.notifyDataSetChanged()
    }
    private suspend fun loadVideoData(sourcePath: String, groupName: String) {

        val favoriteChannels = Common.getChannels(this)

        if (videoListFull.isNotEmpty()) {
            adapter.updateData(videoListFull)
            progressBar.visibility = View.GONE
            return
        }

        videoListFull = sourcePath.split(";")
            .filter { it.isNotEmpty() }
            .map { uriString ->
                val uri = android.net.Uri.parse(uriString)
                val channel = Channel(
                    name = getFileName(uriString) ?: "Unnamed Video",
                    logoUrl = "logoUrl",
                    streamUrl = uri.toString(),
                    isFavorite = false
                )
                val isFavorite = favoriteChannels.any { it.streamUrl == channel.streamUrl && it.name == channel.name }
                channel.isFavorite = isFavorite
                channel
            }.toMutableList()

        adapter.updateData(videoListFull)
    }



    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        sortIcon.isEnabled = false
        sortIcon.alpha = 0.5f
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        sortIcon.isEnabled = true
        sortIcon.alpha = 1f
    }

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
            null
        } catch (e: Exception) {
            Log.e("VideoDetailActivity", "getFileName: Error accessing URI: $uriString, ${e.message}")
            null
        }
    }


    private fun toggleSearchBar() {
    }

    private fun filterVideos(query: String) {
        showLoading()
        val filteredList = if (query.isEmpty()) {
            videoListFull
        } else {
            videoListFull.filter { it.name.contains(query, ignoreCase = true) }
        }
        val sortedList = when (currentSortMode) {
            "AZ" -> filteredList.sortedBy { it.name }
            "ZA" -> filteredList.sortedByDescending { it.name }
            else -> filteredList
        }

        adapter.updateData(sortedList.toMutableList())

        if (sortedList.isEmpty()) {
            imgNotFound.visibility = View.VISIBLE
            txtNotFound.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
            txtNotFound.text = getString(R.string.not_found, query)
        } else {
            imgNotFound.visibility = View.GONE
            txtNotFound.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            recyclerView.scrollToPosition(0)
        }
        hideLoading()
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