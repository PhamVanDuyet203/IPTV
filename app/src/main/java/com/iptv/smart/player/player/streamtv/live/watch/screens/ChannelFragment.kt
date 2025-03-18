package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Bundle
import android.provider.Settings
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
import androidx.appcompat.app.AlertDialog
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

    private var isLoading = false

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
    private var noInternetDialog: AlertDialog? = null
    private var connectivityManager: ConnectivityManager? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null

    private val addPlaylistLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
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
            requireActivity(),
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

        setupNetworkMonitoring()
        checkInternetConnection()
        refreshData()

        return view
    }

    private fun updateChannelList(tabPosition: Int) {
        setTabsLoadingState(true)
        when (tabPosition) {
            0 -> channelsProvider.channels.value?.let {
                updateUI(it)
                setTabsLoadingState(false)
            }
            1 -> channelsProvider.filterChannels(requireContext(), "favorite")
            2 -> channelsProvider.filterChannels(requireContext(), "recent")
        }
    }

    private fun toggleFavorite(channel: Channel) {
        channelsProvider.toggleFavorite(requireContext(), channel)
        if (selectedTabIndex == 1) {
            channelsProvider.filterChannels(requireContext(), "favorite")
        }
    }

    fun refreshData() {
        loadingPanel.visibility = View.VISIBLE
        setTabsLoadingState(true)
        channelsProvider.fetchChannelsFromRoom()
//        channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
//            updateChannelList(selectedTabIndex)
//        }
    }

    private fun setupNetworkMonitoring() {
        val context = context ?: return
        connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                requireActivity().runOnUiThread {
                    dismissNoInternetDialog()
                    loadingPanel.visibility = View.VISIBLE
                    setTabsLoadingState(true)
                    channelsProvider.fetchChannelsFromRoom()
//                    updateChannelList(selectedTabIndex)
//                    setTabsLoadingState(false)
                }
            }

            override fun onLost(network: Network) {
                requireActivity().runOnUiThread {
                    showNoInternetDialog()
                }
            }
        }

        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            .build()

        networkCallback?.let {
            connectivityManager?.registerNetworkCallback(networkRequest, it)
        }
    }

    private fun checkInternetConnection() {
        if (!isInternetAvailable()) {
            showNoInternetDialog()
        } else {
            dismissNoInternetDialog()
            loadingPanel.visibility = View.VISIBLE
            setTabsLoadingState(true)
            channelsProvider.fetchChannelsFromRoom()
//            setTabsLoadingState(false)
        }
    }

    private fun isInternetAvailable(): Boolean {
        val context = context ?: return false
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun showNoInternetDialog() {
        val context = context ?: return
        if (noInternetDialog == null || !noInternetDialog!!.isShowing) {
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_no_internet, null)
            noInternetDialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            val width = (320 * context.resources.displayMetrics.density).toInt()
            val height = (312 * context.resources.displayMetrics.density).toInt()
            noInternetDialog?.window?.apply {
                setLayout(width, height)
                setBackgroundDrawableResource(R.drawable.bg_no_connect)
            }

            dialogView.findViewById<TextView>(R.id.btn_Connect).setOnClickListener {
                startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }

            noInternetDialog?.show()
        }
    }

    private fun dismissNoInternetDialog() {
        noInternetDialog?.takeIf { it.isShowing }?.dismiss()
    }

    private fun nextActivity(channel: Channel) {
        if (isInternetAvailable()) {
            (requireActivity() as HomePageActivity).startPlayerActivity(channel)
        } else {
            showNoInternetDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1000) {
            checkInternetConnection()
        }
    }

    override fun onResume() {
        super.onResume()
        checkInternetConnection()
        refreshData()
        channelAdapter.notifyDataSetChanged()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        networkCallback?.let {
            connectivityManager?.unregisterNetworkCallback(it)
        }
        networkCallback = null
        connectivityManager = null
        dismissNoInternetDialog()
        noInternetDialog = null
        tabViews.clear()
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
                        if (isInternetAvailable()) {
                            widget.setBackgroundColor(android.graphics.Color.WHITE)
                            ImportPlaylistDialog().show(
                                parentFragmentManager,
                                "ImportPlaylistDialog"
                            )
                            widget.postDelayed({
                                widget.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                            }, 200)
                        } else {
                            showNoInternetDialog()
                        }
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
                if (isInternetAvailable()) {
                    selectTab(index)
                } else {
                    showNoInternetDialog()
                }
            }

            tabViews.add(tabView)
            tabContainer.addView(tabView)
        }
    }

    private fun selectTab(index: Int) {
        if (index == selectedTabIndex) return

        tabViews[selectedTabIndex].apply {
            setBackgroundResource(R.drawable.tab_background)
            findViewById<TextView>(R.id.tab_text)?.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
            findViewById<ImageView>(R.id.tab_icon)?.visibility = View.GONE
        }

        tabViews[index].apply {
            setBackgroundResource(R.drawable.tab_selected_background)
            findViewById<TextView>(R.id.tab_text)?.typeface =
                ResourcesCompat.getFont(requireContext(), R.font.inter_bold)
            findViewById<ImageView>(R.id.tab_icon)?.visibility = View.VISIBLE
        }

        selectedTabIndex = index
        channelsProvider.setTabPosition(index)

        if (index == 0) {
            loadingPanel.visibility = View.VISIBLE
            recyclerView.visibility = View.GONE
        }

        updateChannelList(index)
        recyclerView.scrollToPosition(0)

    }


    private fun setTabsLoadingState(isLoading: Boolean) {
        this.isLoading = isLoading
        tabViews.forEachIndexed { index, tabView ->
            if (isLoading) {
                tabView.alpha = if (index == selectedTabIndex) 1f else 0.5f
                tabView.isClickable = false
                tabView.isEnabled = false
            } else {
                tabView.alpha = 1f
                tabView.isClickable = true
                tabView.isEnabled = true
            }
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

            recyclerView.post {
                channelAdapter.notifyDataSetChanged()
            }

        }
    }

    private fun observeChannels() {
        channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
            loadingPanel.visibility = View.GONE
            if (selectedTabIndex == 0) {
                updateUI(channels ?: emptyList())
                setTabsLoadingState(false)
            }
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { filteredChannels ->
            loadingPanel.visibility = View.GONE
            if (selectedTabIndex != 0) {
                updateUI(filteredChannels ?: emptyList())
                setTabsLoadingState(false)
            }
        }

        channelsProvider.error.observe(viewLifecycleOwner) { error ->
            if (!error.isNullOrEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_LONG).show()
                loadingPanel.visibility = View.GONE
                updateUI(emptyList())
                setTabsLoadingState(false)
            }
        }

        channelsProvider.shouldRefresh.observe(viewLifecycleOwner) { shouldRefresh ->
            if (shouldRefresh == true && isInternetAvailable()) {
                loadingPanel.visibility = View.VISIBLE
                setTabsLoadingState(true)
                channelsProvider.fetchChannelsFromRoom(true)
//                channelsProvider.channels.observe(viewLifecycleOwner) { channels ->
//                    updateChannelList(selectedTabIndex)
//                    setTabsLoadingState(false)
//                }
                channelsProvider.resetRefresh()
            }
        }
    }


    fun openAddPlaylistActivity(type: String) {
        if (isInternetAvailable()) {
            val intent = when (type) {
                "URL" -> Intent(requireContext(), ActivityImportPlaylistUrl::class.java)
                "FILE" -> Intent(requireContext(), ActivityImportPlaylistM3U::class.java)
                "DEVICE" -> Intent(requireContext(), ActivityAddPlaylistFromDevice::class.java)
                else -> return
            }
            addPlaylistLauncher.launch(intent)
        } else {
            showNoInternetDialog()
        }
    }
}