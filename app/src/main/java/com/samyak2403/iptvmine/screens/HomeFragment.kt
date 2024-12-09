package com.samyak2403.iptvmine.screens

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.net.Uri
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
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.ChannelsAdapter
import com.samyak2403.iptvmine.model.Channel
import com.samyak2403.iptvmine.provider.ChannelsProvider
import java.io.BufferedReader
import java.io.InputStreamReader


class HomeFragment : Fragment() {

    private lateinit var channelsProvider: ChannelsProvider
    private lateinit var searchEditText: EditText
    private lateinit var searchIcon: ImageView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChannelsAdapter
    private lateinit var toolbar_title: TextView

    private var debounceHandler: Handler? = null
    private var isSearchVisible: Boolean = false

    private val startForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val uri: Uri? = result.data?.data
            if (uri != null) {
                // Đọc nội dung file .m3u từ URI
                activity?.let { channelsProvider.readM3UFileFromUri(it,uri) }
            }
        }
    }



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        channelsProvider = ViewModelProvider(this).get(ChannelsProvider::class.java)
        searchEditText = view.findViewById(R.id.searchEditText)
        searchIcon = view.findViewById(R.id.search_icon)
        progressBar = view.findViewById(R.id.progressBar)
        recyclerView = view.findViewById(R.id.recyclerView)
        toolbar_title = view.findViewById(R.id.toolbar_title)

        adapter = ChannelsAdapter(emptyList()) { channel: Channel ->
            Log.d("==Stream==", "onCreateView: ${channel.streamUrl}")
            PlayerActivity.start(requireContext(), channel)
        }

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        setupObservers()
//        fetchData()

        // Set click listener to toggle the search bar visibility
        searchIcon.setOnClickListener {
            toggleSearchBar()
        }

        toolbar_title.setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/x-mpegurl" // MIME type cho file .m3u
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

    private fun setupObservers() {
        channelsProvider.channels.observe(viewLifecycleOwner, Observer { data ->
            adapter.updateChannels(data)
        })

        channelsProvider.filteredChannels.observe(viewLifecycleOwner, Observer { data ->
            adapter.updateChannels(data)
        })
    }

    private fun fetchData() {
        progressBar.visibility = View.VISIBLE
        channelsProvider.fetchM3UFile()
        progressBar.visibility = View.GONE
    }

    private fun filterChannels(query: String) {
        channelsProvider.filterChannels(query)
    }

    private fun toggleSearchBar() {
        if (isSearchVisible) {
            searchEditText.visibility = View.GONE
            isSearchVisible = false
        } else {
            searchEditText.visibility = View.VISIBLE
            isSearchVisible = true
        }
    }

}




