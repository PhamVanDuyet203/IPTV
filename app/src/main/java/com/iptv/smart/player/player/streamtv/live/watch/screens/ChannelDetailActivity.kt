package com.iptv.smart.player.player.streamtv.live.watch

import android.annotation.SuppressLint
import android.content.Intent
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
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.adapter.ChannelsAdapter
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_SELECT_CATEG_OR_CHANNEL
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityPlaylistDetailBinding
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.screens.PlayerActivity
import com.iptv.smart.player.player.streamtv.live.watch.util.parseM3U
import com.iptv.smart.player.player.streamtv.live.watch.util.parseM3UFromFile
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelDetailActivity : BaseActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelsAdapter
    private lateinit var tvTitle: TextView
    private lateinit var btnBack: ImageView
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var sortIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var frNative: FrameLayout
    private lateinit var vLine: View

    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false
    private var currentSortMode = "AZ"
    private lateinit var channelsProvider: ChannelsProvider
    private var groupName: String = "Unknown"
    private var currentQuery: String = ""
    private lateinit var imgNotFound: ImageView
    private lateinit var txtNotFound: TextView

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

        frNative = findViewById(R.id.fr_home)
        vLine = findViewById(R.id.line)

        imgNotFound = findViewById(R.id.imgNotFound)
        txtNotFound = findViewById(R.id.txtNotFound)
        imgNotFound.visibility = View.GONE
        txtNotFound.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        channelsProvider.init(this)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChannelsAdapter(
            channels = mutableListOf(),
            onChannelClicked = { channel ->
                startAds(channel)
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

    private fun nextActivity(channel: Channel) {
        PlayerActivity.start(this, channel)
        channelsProvider.addToRecent(channel)
    }

    private fun startAds(channel: Channel) {
        when (RemoteConfig.INTER_SELECT_CATEG_OR_CHANNEL_050325) {
            "0" -> {
                nextActivity(channel)
            }
            else -> {
                Common.countInterSelect++
                if (Common.countInterSelect % RemoteConfig.INTER_SELECT_CATEG_OR_CHANNEL_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(this, INTER_SELECT_CATEG_OR_CHANNEL) {
                        nextActivity(channel)
                    }
                } else {
                    nextActivity(channel)
                }
            }
        }

    }

    private fun loadChannels(sourcePath: String) {
        CoroutineScope(Dispatchers.Main).launch {
            progressBar.visibility = View.VISIBLE
            try {
                val channels = withContext(Dispatchers.IO) {
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

    override fun onStart() {
        super.onStart()
        if (RemoteConfig.BANNER_DETAIL_PLAYLIST_CHANNEL_050325 == "1") {
            AdsManager.showAdBannerCollapsible(this, AdsManager.BANNER_DETAIL_PLAYLIST_CHANNEL, frNative , vLine)
        }
        else {
            frNative.gone()
            vLine.gone()
        }
    }


    private fun filterChannels(query: String) {
        CoroutineScope(Dispatchers.IO).launch{
            val allChannels = channelsProvider.channels.value ?: emptyList()
            val filteredList = allChannels.filter { it.groupTitle == groupName }
                .let { list ->
                    if (query.isEmpty()) list
                    else list.filter { it.name.contains(query, ignoreCase = true) }
                }

            val sortedList = when (currentSortMode) {
                "AZ" -> filteredList.sortedBy { it.name }
                "ZA" -> filteredList.sortedByDescending { it.name }
                else -> filteredList
            }

            Log.d("ChannelDetailActivity", "Filtered ${sortedList.size} channels with query: $query")

            withContext(Dispatchers.Main){
                adapter.updateChannels(sortedList)

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
            }
        }


    }


    private fun toggleSearchBar() {

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
        val updatedChannel = channel.copy(isFavorite = !channel.isFavorite)
        channelsProvider.toggleFavorite(updatedChannel,false )
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

            if (sortedList.isEmpty()) {
                imgNotFound.visibility = View.VISIBLE
                txtNotFound.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                imgNotFound.visibility = View.GONE
                txtNotFound.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                recyclerView.scrollToPosition(0)
            }
        }
    }
}