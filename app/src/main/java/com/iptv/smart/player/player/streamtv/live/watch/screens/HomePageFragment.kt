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
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.PlaylistAdapter
import com.iptv.smart.player.player.streamtv.live.watch.dialog.ImportPlaylistDialog
import com.iptv.smart.player.player.streamtv.live.watch.viewmodel.PlaylistViewModel

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

        playlistViewModel.setTabLayout(tabLayout) // Truyền TabLayout vào ViewModel

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

    private fun setupAddButton(view: View) {
        val textView2 = view.findViewById<TextView>(R.id.textView2)
        val fullText = getString(R.string.press_the_add_button_to_create_one_channel)
        val spannableString = SpannableString(fullText)

        val addButtonText = getString(R.string.add_button)
        val startIndex = fullText.indexOf(addButtonText)
        val endIndex = startIndex + addButtonText.length

        if (startIndex != -1) {
            // Đổi màu "Add button" thành @color/cool_blue
            spannableString.setSpan(
                ForegroundColorSpan(getResources().getColor(R.color.add_btn)),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Làm "Add button" thành bold
            spannableString.setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // Thêm sự kiện click cho "Add button" với background trắng khi nhấn
            spannableString.setSpan(
                object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        // Thay đổi background thành trắng khi nhấn
                        widget.setBackgroundColor(android.graphics.Color.WHITE)
                        // Hiển thị dialog
                        ImportPlaylistDialog().show(parentFragmentManager, "ImportPlaylistDialog")
                        // Đặt lại background sau khi nhấn (tùy chọn)
                        widget.postDelayed({
                            widget.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        }, 200) // Thời gian delay 200ms để hiệu ứng nhấn rõ hơn
                    }

                    override fun updateDrawState(ds: android.text.TextPaint) {
                        ds.isUnderlineText = false // Ẩn gạch chân
                        ds.color = getResources().getColor(R.color.add_btn) // Giữ màu cool_blue
                    }
                },
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        textView2.text = spannableString
        textView2.movementMethod = LinkMovementMethod.getInstance() // Bật hỗ trợ click
        textView2.highlightColor = android.graphics.Color.TRANSPARENT // Loại bỏ highlight mặc định
    }

    private fun setupTabLayout() {
        Log.d("HomePageFragment", "Setting up tabs")
        val tabTitles = listOf(getString(R.string.all), getString(R.string.url),
            getString(R.string.file), getString(R.string.gallery))
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
                tabIcon.visibility = View.VISIBLE
            } else {
                tabIcon.visibility = View.GONE
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
                    if (tab.isSelected){
                        val tabIcon = customView?.findViewById<ImageView>(R.id.tab_icon)
                        tabIcon?.visibility = View.VISIBLE
                    }else{
                        val tabIcon = customView?.findViewById<ImageView>(R.id.tab_icon)
                        tabIcon?.visibility = View.GONE
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val customView = it.customView
                    customView?.setBackgroundResource(R.drawable.tab_background)
                    val tabText = customView?.findViewById<TextView>(R.id.tab_text)
                    tabText?.typeface = ResourcesCompat.getFont(requireContext(), R.font.inter_regular)
                    val tabIcon = customView?.findViewById<ImageView>(R.id.tab_icon)
                    tabIcon?.visibility = View.GONE
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }
}