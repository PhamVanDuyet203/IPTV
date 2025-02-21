package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.samyak2403.iptvmine.R

class HomePageFragment : Fragment() {

    private lateinit var tabLayout: TabLayout

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_homepage, container, false)
        tabLayout = view.findViewById(R.id.tabLayout)

        // Danh sách tab
        val tabTitles = listOf("All", "URL", "File", "Gallery")
        val tabIcons = listOf(R.drawable.icon_check, null, null, null) // Chỉ tab "All" có icon

        for (i in tabTitles.indices) {
            val tab = tabLayout.newTab()
            val customView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_tab, null)

            val tabText = customView.findViewById<TextView>(R.id.tab_text)
            val tabIcon = customView.findViewById<ImageView>(R.id.tab_icon)

            tabText.text = tabTitles[i]

            tabIcons[i]?.let {
                tabIcon.setImageResource(it)
                tabIcon.visibility = View.VISIBLE
            }

            tab.customView = customView
            tabLayout.addTab(tab, i == 0) // Tab đầu tiên được chọn mặc định
            // Đặt màu nền mặc định cho tab "All"
            if (i == 0) {
                customView.setBackgroundResource(R.drawable.tab_selected_background)
            }
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.customView?.setBackgroundResource(R.drawable.tab_selected_background)
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.customView?.setBackgroundResource(R.drawable.tab_background)
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })


        return view
    }
}

