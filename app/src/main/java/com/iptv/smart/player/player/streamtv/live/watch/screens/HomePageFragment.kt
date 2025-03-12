package com.iptv.smart.player.player.streamtv.live.watch.screens

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
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.PlaylistAdapter
import com.iptv.smart.player.player.streamtv.live.watch.dialog.ImportPlaylistDialog
import com.iptv.smart.player.player.streamtv.live.watch.viewmodel.PlaylistViewModel

class HomePageFragment : Fragment() {

    private lateinit var tabContainer: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var playlistAdapter: PlaylistAdapter
    private val playlistViewModel: PlaylistViewModel by viewModels()
    private val tabViews = mutableListOf<View>()
    private var selectedTabIndex = 0
    private val sourceTypes = listOf(null, "URL", "FILE", "DEVICE")
    private val tabTitles by lazy {
        listOf(
            getString(R.string.all),
            getString(R.string.url),
            getString(R.string.file),
            getString(R.string.gallery)
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        Log.d("HomePageFragment", "onCreateView called")
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)
        tabContainer = view.findViewById(R.id.tabContainer)
        recyclerView = view.findViewById(R.id.rvPlaylist)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)

        playlistAdapter = PlaylistAdapter(
            requireActivity(),
            emptyList(),
            onDeletePlaylist = { playlist ->
                playlistViewModel.deletePlaylist(playlist)
            },
            onRenamePlaylist = { playlist, newName ->
                playlistViewModel.updatePlaylist(playlist.copy(name = newName))
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = playlistAdapter

        // Thiết lập TextView "Add button" với màu và click
        setupAddButton(view)

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

        setupTabs()

        return view
    }

    override fun onResume() {
        super.onResume()
        Log.d("HomePageFragment", "onResume called")
        playlistViewModel.filterPlaylists(sourceTypes[selectedTabIndex]) // Làm mới dựa trên tab hiện tại
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
        Log.d("HomePageFragment", "Setting up tabs")

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
        playlistViewModel.filterPlaylists(sourceTypes[index])
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tabViews.clear()
    }
}