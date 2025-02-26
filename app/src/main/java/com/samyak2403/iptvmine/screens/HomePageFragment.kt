package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.PlaylistAdapter
import com.samyak2403.iptvmine.viewmodel.PlaylistViewModel

class HomePageFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var playlistAdapter: PlaylistAdapter
    private val playlistViewModel: PlaylistViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomePageFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)
        recyclerView = view.findViewById(R.id.rvPlaylist)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)

        playlistAdapter = PlaylistAdapter(emptyList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = playlistAdapter

        playlistViewModel.filteredPlaylists.observe(viewLifecycleOwner) { playlists ->
            Log.d("HomePageFragment", "Playlists observed: ${playlists.size}")
            if (playlists.isEmpty()) {
                emptyStateLayout.visibility = View.VISIBLE
                recyclerView.visibility = View.GONE
            } else {
                emptyStateLayout.visibility = View.GONE
                recyclerView.visibility = View.VISIBLE
                playlistAdapter.updateData(playlists)
            }
        }

        setupTabLayout()

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomePageFragment", "onResume called")
        val currentTab = tabLayout.selectedTabPosition
        val sourceTypes = listOf(null, "URL", "FILE", "DEVICE")
        playlistViewModel.filterPlaylists(sourceTypes[currentTab]) // Làm mới dựa trên tab hiện tại
    }

    private fun setupTabLayout() {
        Log.d("HomePageFragment", "Setting up tabs")
        val tabTitles = listOf("All", "URL", "File", "Gallery")
        val sourceTypes = listOf(null, "URL", "FILE", "DEVICE")

        for (i in tabTitles.indices) {
            val tab = tabLayout.newTab()
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)
            val tabText = customView.findViewById<TextView>(R.id.tab_text)
            val tabIcon = customView.findViewById<ImageView>(R.id.tab_icon) // Nếu không dùng thì có thể bỏ

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
                    val index = it.position
                    playlistViewModel.filterPlaylists(sourceTypes[index])
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
}