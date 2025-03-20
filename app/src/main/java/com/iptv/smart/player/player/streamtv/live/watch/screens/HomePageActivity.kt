package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.admob.max.dktlibrary.AppOpenManager
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityHomepageBinding
import com.iptv.smart.player.player.streamtv.live.watch.dialog.ImportPlaylistDialog
import com.iptv.smart.player.player.streamtv.live.watch.model.Channel
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import kotlin.system.exitProcess

class HomePageActivity : BaseActivity() {

    private lateinit var binding: ActivityHomepageBinding

    private val selectedColor = 0xFF0095F3.toInt()
    private val defaultTextColor = 0xFF6F797A.toInt()
    private val defaultIconColor = 0xFF000000.toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomepageBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguageCode = sharedPreferences.getString("selectedLanguage", "en") ?: "en"
        updateLanguageIcon(selectedLanguageCode)

        if (!Common.checkBoolAndroid13(this)) {
            Common.checkAndroid13(this)
        }
        replaceFragment(HomePageFragment())
        setSelected(binding.navHome)
        Common.isCheckChannel = false

        binding.navHome.setOnClickListener {
            replaceFragment(HomePageFragment())
            setSelected(binding.navHome)
        }

        binding.languageIcon.setOnClickListener {
            nextActivity()
        }

        binding.navChannel.setOnClickListener {
            replaceFragment(ChannelFragment())
            setSelected(binding.navChannel)
        }

        binding.navCenter.setOnClickListener {
            ImportPlaylistDialog().show(supportFragmentManager, "ImportPlaylistDialog")
        }
    }

    private fun nextActivity() {
        val intent = Intent(this, LanguageSelectionActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finishAffinity()
        exitProcess(0)
    }

    fun startPlayerActivity(channel: Channel) {
        val intent = Intent(this, PlayerActivity::class.java).apply {
            putExtra("channel", channel)
            putExtra("FROMCHANNEL", true)
        }
        playerActivityResultLauncher.launch(intent)
    }

    private val playerActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val shouldRefresh = data?.getBooleanExtra("REFRESH_DATA", false) ?: false
                val channelName = data?.getStringExtra("CHANNEL_NAME") ?: "Unknown"
                val isFavorite = data?.getBooleanExtra("IS_FAVORITE", false) ?: false
                if (shouldRefresh) {
                    refreshCurrentFragment()
                }
            } else {
                Log.d(
                    "HomePageActivity",
                    "Received result from PlayerActivity with resultCode: ${result.resultCode}"
                )
            }
        }

    private fun refreshCurrentFragment() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
        if (currentFragment is ChannelFragment) {
            currentFragment.refreshData()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        when (RemoteConfig.ADS_HOME_050325) {
            "1" -> {
                AdsManager.showAdsBanner(this, AdsManager.BANNER_HOME, binding.frHome, binding.line)
            }

            "2" -> {
                AdsManager.showAdBannerCollapsible(
                    this,
                    AdsManager.BANNER_COLLAP_HOME,
                    binding.frHome,
                    binding.line
                )
            }

            else -> {
                binding.frHome.gone()
                binding.line.gone()
            }
        }

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
            else -> R.drawable.ic_english
        }
        binding.languageIcon.setImageResource(iconResId)
    }
}