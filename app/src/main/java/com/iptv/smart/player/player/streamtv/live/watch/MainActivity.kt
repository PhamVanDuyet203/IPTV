package com.iptv.smart.player.player.streamtv.live.watch

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.screens.HomeFragment
import com.iptv.smart.player.player.streamtv.live.watch.screens.LanguageSelectionActivity
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common
import java.util.Locale

class MainActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selectedLanguage", null)

        if (selectedLanguage != null) {
            applyLocale(selectedLanguage)
        }
        super.onCreate(savedInstanceState)

        if (selectedLanguage == null) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, HomeFragment())
            }
        }
        if (!Common.checkBoolAndroid13(this)) {
            Common.checkAndroid13(this)
        }
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}