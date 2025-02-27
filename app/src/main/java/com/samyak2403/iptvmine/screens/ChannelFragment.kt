package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
import com.samyak2403.iptvmine.dialog.ImportPlaylistDialog
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider

class ChannelFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var loadingPanel: RelativeLayout
    private lateinit var channelAdapter: ChannelsAdapter
    private val channelsProvider: ChannelsProvider by viewModels()



    private val addPlaylistLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("ChannelFragment", "Playlist added, forcing refresh")
            loadingPanel.visibility = View.VISIBLE
            channelsProvider.fetchChannelsFromRoom()
            updateChannelList(tabLayout.selectedTabPosition)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.recyclerViewChannels)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingPanel = view.findViewById(R.id.loadingPanel)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        channelAdapter = ChannelsAdapter(
            channels = mutableListOf(),
            onChannelClicked = { channel -> nextActivity(channel)},
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
        setupAddButton(view)
        observeChannels()

        loadingPanel.visibility = View.VISIBLE
        channelsProvider.fetchChannelsFromRoom()

        return view
    }
    private fun nextActivity(channel: Channel){
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("channel", channel)
        }
        startActivityForResult(intent,1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            Log.d("TAGLOADATAAAAA", "onActivityResult: Reloading data after PlayerActivity")
            loadingPanel.visibility = View.VISIBLE
            channelsProvider.fetchChannelsFromRoom(true)
            channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
                val currentTab = tabLayout.selectedTabPosition
                if (currentTab == 1) {
                    channelsProvider.filterChannels("favorite")
                } else if (currentTab == 2) {
                    channelsProvider.filterChannels("recent")
                }
                updateChannelList(currentTab)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateChannelList(tabLayout.selectedTabPosition)
    }

    private fun setupAddButton(view: View) {
        val textView2 = view.findViewById<TextView>(R.id.textView2)
        val fullText = getString(R.string.press_the_add_button_to_create_one_channel)
        val spannableString = SpannableString(fullText)

        val addButtonText = "Add button"
        val startIndex = fullText.indexOf(addButtonText)
        val endIndex = startIndex + addButtonText.length

        if (startIndex != -1) {
            spannableString.setSpan(
                ForegroundColorSpan(resources.getColor(R.color.add_btn)),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        widget.setBackgroundColor(android.graphics.Color.WHITE)
                        ImportPlaylistDialog().show(parentFragmentManager, "ImportPlaylistDialog")
                        widget.postDelayed({
                            widget.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }, 200)
                    }
                    override fun updateDrawState(ds: android.text.TextPaint) {
                        ds.isUnderlineText = false
                        ds.color = resources.getColor(R.color.add_btn)
                    }
                },
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView2.text = spannableString
        textView2.movementMethod = LinkMovementMethod.getInstance()
        textView2.highlightColor = android.graphics.Color.TRANSPARENT
    }

    private fun setupTabs() {
        val tabTitles = listOf(
            getString(R.string.all_channel),
            getString(R.string.favorite_channel),
            getString(R.string.recent_channel)
        )

        for (i in tabTitles.indices) {
            val tab = tabLayout.newTab()
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)
            val tabText = customView.findViewById<TextView>(R.id.tab_text)
            val tabIcon = customView.findViewById<ImageView>(R.id.tab_icon)

            tabText.text = tabTitles[i]
            tab.customView = customView
            tabLayout.addTab(tab, i == 0)

            if (i == 0) {
                customView.setBackgroundResource(R.drawable.tab_selected_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
                tabIcon.visibility = View.VISIBLE
            } else {
                customView.setBackgroundResource(R.drawable.tab_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
                tabIcon.visibility = View.GONE
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.apply {
                    setBackgroundResource(R.drawable.tab_selected_background)
                    findViewById<TextView>(R.id.tab_text).typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
                    findViewById<ImageView>(R.id.tab_icon).visibility = View.VISIBLE
                }
                tab?.position?.let {
                    channelsProvider.setTabPosition(it)
                    updateChannelList(it)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.apply {
                    setBackgroundResource(R.drawable.tab_background)
                    findViewById<TextView>(R.id.tab_text).typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
                    findViewById<ImageView>(R.id.tab_icon).visibility = View.GONE
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updateChannelList(tabPosition: Int) {
        when (tabPosition) {
            0 -> channelsProvider.channels.value?.let { updateUI(it) }
            1 -> channelsProvider.filterChannels("favorite")
            2 -> channelsProvider.filterChannels("recent")
        }
    }

    private fun updateUI(channels: List<Channel>) {
        loadingPanel.visibility = View.GONE
        if (channels.isEmpty()) {
            emptyStateLayout.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        } else {
            emptyStateLayout.visibility = View.GONE
            recyclerView.visibility = View.VISIBLE
            channelAdapter.updateChannels(channels)
        }
    }

    private fun observeChannels() {
        channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
            loadingPanel.visibility = View.GONE
            if (tabLayout.selectedTabPosition == 0) {
                updateUI(channels ?: emptyList())
            }
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { filteredChannels ->
            loadingPanel.visibility = View.GONE
            if (tabLayout.selectedTabPosition != 0) {
                updateUI(filteredChannels ?: emptyList())
            }
        }

        channelsProvider.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                loadingPanel.visibility = View.GONE
                updateUI(emptyList())
            }
        }

        // Quan sát yêu cầu làm mới từ ChannelsProvider
        channelsProvider.shouldRefresh.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh == true) {
                Log.d("ChannelFragment", "Refresh requested, forcing reload of channels")
                loadingPanel.visibility = View.VISIBLE
                channelsProvider.fetchChannelsFromRoom(true) // Buộc làm mới dữ liệu
                channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
                    // Sau khi dữ liệu được tải lại, cập nhật giao diện
                    updateChannelList(tabLayout.selectedTabPosition)
                }
                channelsProvider.resetRefresh() // Reset flag sau khi yêu cầu làm mới
            }
        }
    }

    private fun toggleFavorite(channel: Channel) {
        channelsProvider.toggleFavorite(channel)
    }

    fun openAddPlaylistActivity(type: String) {
        val intent = when (type) {
            "URL" -> Intent(requireContext(), ActivityImportPlaylistUrl::class.java)
            "FILE" -> Intent(requireContext(), ActivityImportPlaylistM3U::class.java)
            "DEVICE" -> Intent(requireContext(), ActivityAddPlaylistFromDevice::class.java)
            else -> return
        }
        addPlaylistLauncher.launch(intent)
    }
}