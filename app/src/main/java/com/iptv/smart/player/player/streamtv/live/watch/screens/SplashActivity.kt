package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.postDelayed
import com.admob.max.dktlibrary.AOAManager
import com.admob.max.dktlibrary.AdmobUtils
import com.admob.max.dktlibrary.AppOpenManager
import com.admob.max.dktlibrary.cmp.GoogleMobileAdsConsentManager
import com.google.android.gms.ads.AdValue
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivitySplashBinding
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

class SplashActivity : BaseActivity() {
    private val binding by lazy { ActivitySplashBinding.inflate(layoutInflater) }
    private var isMobileAdsInitializeCalled = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)



        if (AdmobUtils.isNetworkConnected(this)) {
            getKeyRemoteConfig()
        } else {
            Handler(Looper.getMainLooper()).postDelayed({
                nextActivity()
            }, 3000)
        }
    }

    private fun nextActivity() {
        val intent = Intent(this, LanguageSelectionActivity::class.java)
        intent.putExtra("FROMSPLASH", true)

        intent.putExtra("btn_back", "GONE")



        intent.flags =
            Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun getKeyRemoteConfig() {
        RemoteConfig.initRemoteConfig(object : RemoteConfig.CompleteListener {
            override fun onComplete() {
                RemoteConfig.ADS_SPLASH_050325 = RemoteConfig.getValueAbTest("ADS_SPLASH_050325")
                RemoteConfig.BANNER_SPLASH_050325 =
                    RemoteConfig.getValueAbTest("BANNER_SPLASH_050325")
                RemoteConfig.NATIVE_LANGUAGE_050325 =
                    RemoteConfig.getValueAbTest("NATIVE_LANGUAGE_050325")
                RemoteConfig.INTER_LANGUAGE_050325 =
                    RemoteConfig.getValueAbTest("INTER_LANGUAGE_050325")
                RemoteConfig.NATIVE_INTRO_050325 =
                    RemoteConfig.getValueAbTest("NATIVE_INTRO_050325")
                RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 =
                    RemoteConfig.getValueAbTest("NATIVE_FULL_SCREEN_INTRO_050325")
                RemoteConfig.ADS_HOME_050325 = RemoteConfig.getValueAbTest("ADS_HOME_050325")
              //  RemoteConfig.INTER_ADD_050325 = RemoteConfig.getValueAbTest("INTER_ADD_050325")
//                RemoteConfig.NATIVE_ADD_050325 = RemoteConfig.getValueAbTest("NATIVE_ADD_050325")
                RemoteConfig.INTER_SAVE_ADD_050325 =
                    RemoteConfig.getValueAbTest("INTER_SAVE_ADD_050325")
              //  RemoteConfig.INTER_ITEMS_PLAYLIST_050325 =
                    RemoteConfig.getValueAbTest("INTER_ITEMS_PLAYLIST_050325")
              //  RemoteConfig.INTER_SELECT_CATEG_OR_CHANNEL_050325 =
                    RemoteConfig.getValueAbTest("INTER_SELECT_CATEG_OR_CHANNEL_050325")
                RemoteConfig.NATIVE_PLAYLIST_CHANNEL_050325 =
                    RemoteConfig.getValueAbTest("NATIVE_PLAYLIST_CHANNEL_050325")
           //     RemoteConfig.BANNER_DETAIL_PLAYLIST_CHANNEL_050325 =
                    RemoteConfig.getValueAbTest("BANNER_DETAIL_PLAYLIST_CHANNEL_050325")
              //  RemoteConfig.INTER_BACK_PLAY_TO_LIST_050325 =
                    RemoteConfig.getValueAbTest("INTER_BACK_PLAY_TO_LIST_050325")
             //   RemoteConfig.ADS_PLAY_CONTROL_050325 =
                    RemoteConfig.getValueAbTest("ADS_PLAY_CONTROL_050325")
                RemoteConfig.ONRESUME_050325 = RemoteConfig.getValueAbTest("ONRESUME_050325")
                setupCMP()
            }

        })
    }

    private fun setupCMP() {
        val googleMobileAdsConsentManager = GoogleMobileAdsConsentManager(this)
        googleMobileAdsConsentManager.gatherConsent { error ->
            error?.let {
                initializeMobileAdsSdk()
            }

            if (googleMobileAdsConsentManager.canRequestAds) {
                initializeMobileAdsSdk()
            }
        }

    }

    private fun initializeMobileAdsSdk() {
        if (isMobileAdsInitializeCalled.get()) {
            return
        }
        isMobileAdsInitializeCalled.set(true)
        initAdmod()
    }

    private fun initAdmod() {
        AdmobUtils.initAdmob(this, 12000, AdsManager.isDebug, isEnableAds = true)
        if (Build.VERSION.SDK_INT != Build.VERSION_CODES.P && RemoteConfig.ONRESUME_050325 == "1") {
            AppOpenManager.getInstance().init(application, AdsManager.ONRESUME, false)
            AppOpenManager.getInstance().disableAppResumeWithActivity(SplashActivity::class.java)
        }
        if (RemoteConfig.BANNER_SPLASH_050325 == "1") {
            binding.frBanner.visible()
            binding.view.visible()
            AdsManager.showAdsBannerSplash(this,
                AdsManager.BANNER_SPLASH,
                binding.frBanner,
                binding.view,
                object : AdsManager.onDelay {
                    override fun onDelay() {
                        Handler(Looper.getMainLooper()).postDelayed({
                            checkRemoteConFigSPlash()
                        }, 1000)
                    }

                })
        }else{
            checkRemoteConFigSPlash()
        }
        AdsManager.loadAdsNative(this, AdsManager.NATIVE_INTRO)
        AdsManager.loadAdsNative(this, AdsManager.NATIVE_LANGUAGE)
        AdsManager.loadAdsNative(this, AdsManager.NATIVE_LANGUAGE_ID2)
    }


    private fun checkRemoteConFigSPlash() {
        when (RemoteConfig.ADS_SPLASH_050325) {
            "1" -> {
                val aoaManager = AOAManager(this,
                    AdsManager.AOA_SPLASH,
                    20000,
                    object : AOAManager.AppOpenAdsListener {
                        override fun onAdsLoaded() {}
                        override fun onAdsFailed(s: String) {
                            nextActivity()
                        }

                        override fun onAdPaid(adValue: AdValue, adUnitAds: String) {
                            AdsManager.postRevenueAdjust(adValue, adUnitAds)
                        }

                        override fun onAdsClose() {
                            nextActivity()
                        }
                    })
                aoaManager.loadAoA()
            }

            "2" -> {
                AdsManager.loadAndShowInterSplash(this,
                    AdsManager.INTER_SPLASH,
                    object : AdsManager.AdListener {
                        override fun onAdClosed() {
                            nextActivity()
                        }
                    })
            }

            else -> {
                nextActivity()
            }
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