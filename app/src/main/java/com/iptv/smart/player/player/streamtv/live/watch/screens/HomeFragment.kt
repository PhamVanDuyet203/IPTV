package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.ChannelsAdapter
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.provider.ChannelsProvider
import com.iptv.smart.player.player.streamtv.live.watch.utils.NetworkChangeReceiver

class HomeFragment : Fragment() {
    lateinit var networkChangeReceiver: NetworkChangeReceiver

    private lateinit var channelsProvider: ChannelsProvider
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelsAdapter
    private lateinit var toolbarTitle: TextView

    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false

    private val startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    Log.d("HomeFragment", "Selected M3U file URI: $uri")
                    // TODO: Xử lý file M3U từ URI nếu cần, hiện tại fetch từ Room nên bỏ qua
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        channelsProvider = viewModels<ChannelsProvider>().value
        searchEditText = view.findViewById(R.id.searchEditText)
        searchIcon = view.findViewById(R.id.search_icon)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        toolbarTitle = view.findViewById(R.id.toolbar_title)

        adapter = ChannelsAdapter(
            requireActivity(),
            channels = mutableListOf(), // Sửa từ emptyList() thành mutableListOf()
            onChannelClicked = { channel ->
                Log.d("==Stream==", "onCreateView: ${channel.streamUrl}")
                PlayerActivity.start(requireContext(), channel)
            },
            onFavoriteClicked = { channel ->
                channelsProvider.toggleFavorite(requireContext(), channel)
            },
            onRenameChannel = { channel, newName ->
                channelsProvider.updateChannel(channel.copy(name = newName))
            },
            onDeleteChannel = { channel ->
                channelsProvider.deleteChannel(channel)
            })

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        channelsProvider.init(requireContext())
        setupObservers()
        fetchData()

        searchIcon.setOnClickListener { toggleSearchBar() }

        toolbarTitle.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/x-mpegurl"
                addCategory(Intent.CATEGORY_OPENABLE)
            }
            startForResult.launch(intent)
        }

        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                debounceHandler?.removeCallbacksAndMessages(null)
                debounceHandler = Handler(Looper.getMainLooper())
                debounceHandler?.postDelayed({
                    filterChannels(s.toString())
                }, 500)
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        return view
    }

    override fun onStop() {
        super.onStop()
        requireActivity().unregisterReceiver(networkChangeReceiver)

    }

    override fun onStart() {
        super.onStart()
        networkChangeReceiver = NetworkChangeReceiver { isConnected ->
            if (!isConnected) {
                adapter.notifyDataSetChanged()
            } else {
                adapter.notifyDataSetChanged()
            }
        }

        val intentFilter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireActivity().registerReceiver(
                networkChangeReceiver, intentFilter, Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            requireActivity().registerReceiver(networkChangeReceiver, intentFilter)

        }
    }

    private fun setupObservers() {
        channelsProvider.channels.observe(viewLifecycleOwner) { data ->
            adapter.updateChannels(data)
            progressBar.visibility = View.GONE
        }

        channelsProvider.filteredChannels.observe(viewLifecycleOwner) { data ->
            adapter.updateChannels(data)
        }

        channelsProvider.error.observe(viewLifecycleOwner) { error ->
            if (error != null) {
                Log.e("HomeFragment", error)
                progressBar.visibility = View.GONE
            }
        }
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        channelsProvider.fetchChannelsFromRoom()
    }

    private fun filterChannels(query: String) {
        channelsProvider.filterChannels(requireContext(), query)
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            searchEditText.visibility = View.GONE
        } else {
            searchEditText.visibility = View.VISIBLE
        }
        isSearchVisible = !isSearchVisible
    }
}