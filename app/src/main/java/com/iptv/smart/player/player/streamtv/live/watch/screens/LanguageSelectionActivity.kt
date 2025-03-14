package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.admob.max.dktlibrary.utils.admod.NativeHolderAdmob
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.Language
import com.iptv.smart.player.player.streamtv.live.watch.adapter.LanguageAdapter
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityLanguageSelectorBinding
import com.iptv.smart.player.player.streamtv.live.watch.intro.IntroActivity
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import java.util.Locale
import java.util.SimpleTimeZone

class LanguageSelectionActivity : BaseActivity() {

    private lateinit var checkIcon: ImageButton
    private lateinit var adapter: LanguageAdapter
    private val binding by lazy { ActivityLanguageSelectorBinding.inflate(layoutInflater) }

    private var fromSplash: Boolean = false
    private var isFirstLanguageClick = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        fromSplash = intent.getBooleanExtra("FROMSPLASH", false)

        if (fromSplash) {
            binding.icBack.gone()
            if (RemoteConfig.NATIVE_LANGUAGE_050325=="1"){
                AdsManager.showNativeBottom(this, binding.frNative, AdsManager.NATIVE_LANGUAGE)
            }else{
                binding.frNative.gone()
            }
            loadNativeAd()

        } else {
            binding.icBack.visible()
        }



        binding.icBack.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }


        val languageList = findViewById<RecyclerView>(R.id.language_list)
        checkIcon = findViewById(R.id.check_icon)

        languageList.layoutManager = LinearLayoutManager(this)
        val languages = getLanguages()

        adapter = LanguageAdapter(languages, object : LanguageAdapter.onItemClickListener {

            override fun onItemClick() {
                handleNativeAd()
            }
        })
        languageList.adapter = adapter

        if (!fromSplash) {
            val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
            val savedLanguageCode = sharedPreferences.getString("selectedLanguage", null)
            if (savedLanguageCode != null) {
                val savedLanguage = languages.find { it.code == savedLanguageCode }
                savedLanguage?.let {
                    val position = languages.indexOf(it)
                    adapter.updateSelection(position)
                }
            }
        }
        checkIcon.setOnClickListener {
            if (fromSplash) {
                val selectedLanguage = adapter.getSelectedLanguage()
                if (selectedLanguage == null){
                    Toast.makeText(this@LanguageSelectionActivity, "Please select a language first!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (RemoteConfig.INTER_LANGUAGE_050325 == "1") {
                    AdsManager.loadAndShowInter(this, AdsManager.INTER_LANGUAGE) {
                        saveLanguage()
                    }
                } else {
                    saveLanguage()
                }
            } else {
                saveLanguage()
            }

        }

    }

    private fun saveLanguage() {
        val selectedLanguage = adapter.getSelectedLanguage()
        if (selectedLanguage != null) {
            saveLanguageAndProceed(selectedLanguage.code)
        }
    }


    private fun loadNativeAd() {
        if (RemoteConfig.NATIVE_INTRO_050325.contains("1") || RemoteConfig.NATIVE_INTRO_050325.contains("2")
            || RemoteConfig.NATIVE_INTRO_050325.contains("3") || RemoteConfig.NATIVE_INTRO_050325.contains("4")) {
            AdsManager.loadAdsNative(this, AdsManager.NATIVE_INTRO)
        }

        if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.contains("1") || RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.contains("2")
            || RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.contains("3") || RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.contains("4")) {
            AdsManager.loadNativeFullScreen(this, AdsManager.NATIVE_FULL_SCREEN_INTRO)
        }

    }


    fun handleNativeAd() {
        if (isFirstLanguageClick && fromSplash) {
            isFirstLanguageClick = false
            if (RemoteConfig.NATIVE_LANGUAGE_050325 == "1") {
                AdsManager.showNativeBottom(this, binding.frNative, AdsManager.NATIVE_LANGUAGE_ID2)
            } else {
                binding.frNative.gone()
            }
        }
    }

    private fun saveLanguageAndProceed(languageCode: String) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("selectedLanguage", languageCode)
            apply()
        }

        applyLocale(languageCode)

        if (fromSplash) {
            val intent = Intent(this, IntroActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        } else {
            val intent = Intent(this, HomePageActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    override fun onStart() {
        super.onStart()
        if (!fromSplash) {
            if (RemoteConfig.NATIVE_LANGUAGE_050325 == "1") {
                AdsManager.loadAndShowAdsNative(this, binding.frNative, AdsManager.NATIVE_LANGUAGE)
            } else binding.frNative.gone()
        }
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }


    private fun getLanguages(): List<Language> {
        return listOf(
            Language(getString(R.string.english), "en", R.drawable.ic_english),
            Language(getString(R.string.korean), "ko", R.drawable.ic_korean),
            Language(getString(R.string.spanish), "es", R.drawable.ic_spanish),
            Language(getString(R.string.french), "fr", R.drawable.ic_french),
            Language(getString(R.string.arabic), "ar", R.drawable.ic_arabic),
            Language(getString(R.string.bengali), "bn", R.drawable.ic_bengali),
            Language(getString(R.string.russian), "ru", R.drawable.ic_russian),
            Language(getString(R.string.portuguese), "pt", R.drawable.ic_portuguese),
            Language(getString(R.string.indonesia), "in", R.drawable.ic_indonesian),
            Language(getString(R.string.hindi), "hi", R.drawable.ic_hindi),
            Language(getString(R.string.italian), "it", R.drawable.ic_italian),
            Language(getString(R.string.german), "de", R.drawable.ic_german)
        )
    }

    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun View.gone() {
        visibility = View.GONE
    }
}