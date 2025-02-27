package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.databinding.ActivityHomepageBinding
import com.samyak2403.iptvmine.dialog.ImportPlaylistDialog

class HomePageActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomepageBinding

    private val selectedColor = 0xFF0095F3.toInt()
    private val defaultTextColor = 0xFF6F797A.toInt()
    private val defaultIconColor = 0xFF000000.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load ngôn ngữ đã chọn và cập nhật icon
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguageCode = sharedPreferences.getString("selectedLanguage", "en") ?: "en"
        updateLanguageIcon(selectedLanguageCode)

        // Mặc định chọn Home
        replaceFragment(HomePageFragment())
        setSelected(binding.navHome)

        // Xử lý sự kiện nhấn
        binding.navHome.setOnClickListener {
            replaceFragment(HomePageFragment())
            setSelected(binding.navHome)
        }

        binding.languageIcon.setOnClickListener {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
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
        resetNavigation()
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

    private fun updateLanguageIcon(languageCode: String) {
        val iconResId = when (languageCode) {
            "en" -> R.drawable.ic_english
            "ko" -> R.drawable.ic_korean
            "es" -> R.drawable.ic_spanish
            "fr" -> R.drawable.ic_french
            "ar" -> R.drawable.ic_arabic
            "bn" -> R.drawable.ic_bengali
            "ru" -> R.drawable.ic_russian
            "pt" -> R.drawable.ic_portuguese
            "in" -> R.drawable.ic_indonesian
            "hi" -> R.drawable.ic_hindi
            "it" -> R.drawable.ic_italian
            "de" -> R.drawable.ic_german
            else -> R.drawable.ic_english // Mặc định
        }
        binding.languageIcon.setImageResource(iconResId)
    }
}