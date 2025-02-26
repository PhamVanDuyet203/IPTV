package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.databinding.ActivityHomepageBinding
import com.samyak2403.iptvmine.dialog.ImportPlaylistDialog

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    private val selectedColor = 0xFF0095F3.toInt() // Màu #0095F3 khi được chọn
    private val defaultTextColor = 0xFF6F797A.toInt() // Màu mặc định cho text
    private val defaultIconColor = 0xFF000000.toInt() // Màu mặc định cho icon (có thể thay đổi)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Mặc định chọn Home
        replaceFragment(HomePageFragment())
        setSelected(binding.navHome)

        // Xử lý sự kiện nhấn
        binding.navHome.setOnClickListener {
            replaceFragment(HomePageFragment())
            setSelected(binding.navHome)
        }

        binding.navChannel.setOnClickListener {
            replaceFragment(ChannelFragment())
            setSelected(binding.navChannel)
        }

        binding.navCenter.setOnClickListener {
            ImportPlaylistDialog().show(supportFragmentManager, "ImportPlaylistDialog")
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    private fun setSelected(selectedLayout: LinearLayout) {
        // Reset tất cả về trạng thái mặc định
        resetNavigation()

        // Cập nhật trạng thái cho mục được chọn
        when (selectedLayout.id) {
            R.id.nav_home -> {
                binding.homeText.setTextColor(selectedColor)
                binding.homeIcon.setColorFilter(selectedColor)
            }
            R.id.nav_channel -> {
                binding.channelText.setTextColor(selectedColor)
                binding.channelIcon.setColorFilter(selectedColor)
            }
        }
    }

    private fun resetNavigation() {
        binding.homeText.setTextColor(defaultTextColor)
        binding.homeIcon.setColorFilter(defaultIconColor)
        binding.channelText.setTextColor(defaultTextColor)
        binding.channelIcon.setColorFilter(defaultIconColor)
    }
}