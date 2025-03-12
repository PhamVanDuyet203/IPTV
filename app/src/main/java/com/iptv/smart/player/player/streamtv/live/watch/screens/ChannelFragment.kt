package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.ChannelsAdapter
import com.iptv.smart.player.player.streamtv.live.watch.dialog.ImportPlaylistDialog
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider

class ChannelFragment : Fragment() {

    private lateinit var tabContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var loadingPanel: RelativeLayout
    private lateinit var channelAdapter: ChannelsAdapter
    private val channelsProvider: ChannelsProvider by viewModels()
    private val tabViews = mutableListOf<View>()
    private var selectedTabIndex = 0
    private val tabTitles by lazy {
        listOf(
            getString(R.string.all_channel),
            getString(R.string.favorite_channel),
            getString(R.string.recent_channel)
        )
    }

    private val addPlaylistLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            Log.d("ChannelFragment", "Playlist added, forcing refresh")
            loadingPanel.visibility = View.VISIBLE
            channelsProvider.fetchChannelsFromRoom()
            updateChannelList(selectedTabIndex)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_channel, container, false)
        tabContainer = view.findViewById(R.id.tabContainer)
        recyclerView = view.findViewById(R.id.recyclerViewChannels)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        loadingPanel = view.findViewById(R.id.loadingPanel)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        channelAdapter = ChannelsAdapter(
            channels = mutableListOf(),
            onChannelClicked = { channel -> nextActivity(channel) },
            onFavoriteClicked = { channel -> toggleFavorite(channel) },
            onRenameChannel = { channel, newName ->
                channelsProvider.updateChannel(channel.copy(name = newName))
                updateChannelList(selectedTabIndex)
            },
            onDeleteChannel = { channel ->
                channelsProvider.deleteChannel(channel)
                updateChannelList(selectedTabIndex)
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

    private fun nextActivity(channel: Channel) {
        val intent = Intent(requireContext(), PlayerActivity::class.java).apply {
            putExtra("channel", channel)
        }
        startActivityForResult(intent, 1000)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            Log.d("TAGLOADATAAAAA", "onActivityResult: Reloading data after PlayerActivity")
            loadingPanel.visibility = View.VISIBLE
            channelsProvider.fetchChannelsFromRoom(true)
            channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
                if (selectedTabIndex == 1) {
                    channelsProvider.filterChannels("favorite")
                } else if (selectedTabIndex == 2) {
                    channelsProvider.filterChannels("recent")
                }
                updateChannelList(selectedTabIndex)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        updateChannelList(selectedTabIndex)
    }

    private fun setupAddButton(view: View) {
        val textView2 = view.findViewById<TextView>(R.id.textView2)
        val fullText = getString(R.string.press_the_add_button_to_create_one_channel)
        val spannableString = SpannableString(fullText)

        val addButtonText = getString(R.string.add_button)
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

                    override fun updateDrawState(ds: TextPaint) {
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
        tabTitles.forEachIndexed { index, title ->
            val tabView = LayoutInflater.from(requireContext())
                .inflate(R.layout.custom_tab, tabContainer, false)
            val tabText = tabView.findViewById<TextView>(R.id.tab_text)
            val tabIcon = tabView.findViewById<ImageView>(R.id.tab_icon)

            tabText.text = title

            if (index == 0) {
                tabView.setBackgroundResource(R.drawable.tab_selected_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
                tabIcon.visibility = View.VISIBLE
            } else {
                tabView.setBackgroundResource(R.drawable.tab_background)
                tabText.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
                tabIcon.visibility = View.GONE
            }

            tabView.setOnClickListener {
                selectTab(index)
            }

            tabViews.add(tabView)
            tabContainer.addView(tabView)
        }
    }

    private fun selectTab(index: Int) {
        if (index == selectedTabIndex) return

        // Deselect tab cũ
        tabViews[selectedTabIndex].apply {
            setBackgroundResource(R.drawable.tab_background)
            findViewById<TextView>(R.id.tab_text)?.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            findViewById<ImageView>(R.id.tab_icon)?.visibility = View.GONE
        }

        // Select tab mới
        tabViews[index].apply {
            setBackgroundResource(R.drawable.tab_selected_background)
            findViewById<TextView>(R.id.tab_text)?.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            findViewById<ImageView>(R.id.tab_icon)?.visibility = View.VISIBLE
        }

        selectedTabIndex = index
        channelsProvider.setTabPosition(index)
        updateChannelList(index)
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
            if (selectedTabIndex == 0) {
                updateUI(channels ?: emptyList())
            }
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { filteredChannels ->
            loadingPanel.visibility = View.GONE
            if (selectedTabIndex != 0) {
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

        channelsProvider.shouldRefresh.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh == true) {
                Log.d("ChannelFragment", "Refresh requested, forcing reload of channels")
                loadingPanel.visibility = View.VISIBLE
                channelsProvider.fetchChannelsFromRoom(true)
                channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
                    updateChannelList(selectedTabIndex)
                }
                channelsProvider.resetRefresh()
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

    override fun onDestroyView() {
        super.onDestroyView()
        tabViews.clear()
    }
}