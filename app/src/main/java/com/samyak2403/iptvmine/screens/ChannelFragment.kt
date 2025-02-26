package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.ChannelsAdapter
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider

class ChannelFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var loadingPanel: RelativeLayout
    private lateinit var channelAdapter: ChannelsAdapter
    private val channelsProvider: ChannelsProvider by viewModels()
    private val handler = Handler(Looper.getMainLooper()) // Handler để trì hoãn hiển thị empty state
    private var isDataLoaded = false // Cờ để kiểm tra dữ liệu đã tải xong chưa

    private val addPlaylistLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        Log.d("ChannelFragment", "addPlaylistLauncher triggered, resultCode: ${result.resultCode}")
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("ChannelFragment", "Playlist added, forcing refresh")
            loadingPanel.visibility = View.VISIBLE // Hiển thị loading khi làm mới
            isDataLoaded = false
            channelsProvider.fetchChannelsFromRoom()
            val currentTab = tabLayout.selectedTabPosition
            Log.d("ChannelFragment", "Current tab: $currentTab")
            updateChannelList(currentTab)
        } else {
            Log.d("ChannelFragment", "Result not OK, skipping refresh")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        Log.d("ChannelFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerViewChannels)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingPanel = view.findViewById(R.id.loadingPanel)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        channelAdapter = ChannelsAdapter(
            channels = mutableListOf(),
            onChannelClicked = { channel -> PlayerActivity.start(requireContext(), channel) },
            onFavoriteClicked = { channel -> toggleFavorite(channel) },
            onRenameChannel = { channel, newName ->
                channelsProvider.updateChannel(channel.copy(name = newName))
                updateChannelList(tabLayout.selectedTabPosition)
            },
            onDeleteChannel = { channel ->
                channelsProvider.deleteChannel(channel)
                updateChannelList(tabLayout.selectedTabPosition)
            }
        )
        recyclerView.adapter = channelAdapter

        channelsProvider.init(requireContext())
        setupTabs()
        observeChannels()

        // Hiển thị loading và bắt đầu tải dữ liệu
        loadingPanel.visibility = View.VISIBLE
        isDataLoaded = false
        channelsProvider.fetchChannelsFromRoom()

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("ChannelFragment", "onResume called")
        val currentTab = tabLayout.selectedTabPosition
        updateChannelList(currentTab)
    }

    private fun setupTabs() {
        Log.d("ChannelFragment", "Setting up tabs")
        val tabTitles = listOf("All", "Favorite", "Recent")
        for (i in tabTitles.indices) {
            val tab = tabLayout.newTab()
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)
            val tabText = customView.findViewById<TextView>(R.id.tab_text)
            tabText.text = tabTitles[i]
            tab.customView = customView
            tabLayout.addTab(tab, i == 0)
            if (i == 0) {
                customView.setBackgroundResource(R.drawable.tab_selected_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            } else {
                customView.setBackgroundResource(R.drawable.tab_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = it.customView
                    customView?.setBackgroundResource(R.drawable.tab_selected_background)
                    val tabText = customView?.findViewById<TextView>(R.id.tab_text)
                    tabText?.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
                    channelsProvider.setTabPosition(it.position)
                    updateChannelList(it.position)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = it.customView
                    customView?.setBackgroundResource(R.drawable.tab_background)
                    val tabText = customView?.findViewById<TextView>(R.id.tab_text)
                    tabText?.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateChannelList(tabPosition: Int) {
        Log.d("ChannelFragment", "Updating channel list for tab: $tabPosition")
        when (tabPosition) {
            0 -> {
                val channels = channelsProvider.channels.value ?: emptyList()
                updateUI(channels)
            }
            1 -> channelsProvider.filterChannels("favorite")
            2 -> channelsProvider.filterChannels("recent")
        }
    }

    private fun updateUI(channels: List<Channel>) {
        Log.d("ChannelFragment", "Updating UI with channels: ${channels.size}")
        if (isDataLoaded) {
            if (channels.isEmpty()) {
                // Trì hoãn hiển thị emptyStateLayout 3 giây nếu không có dữ liệu
                handler.removeCallbacksAndMessages(null)
                handler.postDelayed({
                    if (channelsProvider.channels.value?.isEmpty() == true) { // Kiểm tra lại sau 3s
                        emptyStateLayout.visibility = View.VISIBLE
                        recyclerView.visibility = View.GONE
                    }
                }, 3000)
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                channelAdapter.updateChannels(channels)
            }
        }
    }

    private fun observeChannels() {
        Log.d("ChannelFragment", "Setting up observers")
        channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
            Log.d("ChannelFragment", "Channels observed: ${channels.size}")
            isDataLoaded = true // Đánh dấu dữ liệu đã tải xong
            loadingPanel.visibility = View.GONE // Ẩn loading khi có dữ liệu
            if (tabLayout.selectedTabPosition == 0) {
                updateUI(channels)
            }
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { filteredChannels ->
            Log.d("ChannelFragment", "Filtered channels observed: ${filteredChannels.size}")
            isDataLoaded = true // Đánh dấu dữ liệu đã tải xong
            loadingPanel.visibility = View.GONE // Ẩn loading khi có dữ liệu
            if (tabLayout.selectedTabPosition != 0) {
                updateUI(filteredChannels)
            }
        }

        channelsProvider.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                Log.e("ChannelFragment", "Error: $error")
                isDataLoaded = true // Đánh dấu dữ liệu đã tải xong dù có lỗi
                loadingPanel.visibility = View.GONE // Ẩn loading nếu có lỗi
                updateUI(emptyList()) // Cập nhật UI với danh sách rỗng để kiểm tra empty state
            }
        }
    }

    private fun toggleFavorite(channel: Channel) {
        channelsProvider.toggleFavorite(channel)
    }

    fun openAddPlaylistActivity(type: String) {
        Log.d("ChannelFragment", "Opening add playlist activity: $type")
        val intent = when (type) {
            "URL" -> Intent(requireContext(), ActivityImportPlaylistUrl::class.java)
            "FILE" -> Intent(requireContext(), ActivityImportPlaylistM3U::class.java)
            "DEVICE" -> Intent(requireContext(), ActivityAddPlaylistFromDevice::class.java)
            else -> return
        }
        addPlaylistLauncher.launch(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null) // Xóa các callback khi view bị hủy
    }
}