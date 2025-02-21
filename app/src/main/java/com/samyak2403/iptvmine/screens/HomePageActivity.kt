package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.databinding.ActivityHomepageBinding

class HomePageActivity : AppCompatActivity() {
    private val binding by lazy { ActivityHomepageBinding.inflate(layoutInflater) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Hiển thị Fragment Home mặc định
        replaceFragment(HomePageFragment())


        // Xử lý khi nhấn vào bottom nav
        findViewById<LinearLayout>(R.id.nav_home).setOnClickListener {
            replaceFragment(HomePageFragment())
        }

        findViewById<LinearLayout>(R.id.nav_channel).setOnClickListener {
            replaceFragment(ChannelFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
