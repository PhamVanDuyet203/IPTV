package com.iptv.smart.player.player.streamtv.live.watch.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustAdRevenue
import com.admob.max.dktlibrary.AdmobUtils
import com.admob.max.dktlibrary.AppOpenManager
import com.admob.max.dktlibrary.CollapsibleBanner
import com.admob.max.dktlibrary.GoogleENative
import com.admob.max.dktlibrary.utils.admod.BannerHolderAdmob
import com.admob.max.dktlibrary.utils.admod.InterHolderAdmob
import com.admob.max.dktlibrary.utils.admod.NativeHolderAdmob
import com.admob.max.dktlibrary.utils.admod.callback.AdCallBackInterLoad
import com.admob.max.dktlibrary.utils.admod.callback.AdsInterCallBack
import com.admob.max.dktlibrary.utils.admod.callback.NativeAdmobCallback
import com.admob.max.dktlibrary.utils.admod.callback.NativeFullScreenCallBack
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.nativead.NativeAd
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.intro.IntroActivity


object AdsManager {
    var isLoadBanner = MutableLiveData(true)
    var isDebug = true
    var isShow = true
    var isTestDevice = false

    var AOA_SPLASH = ""

    var INTER_SPLASH = InterHolderAdmob("")
    var INTER_LANGUAGE = InterHolderAdmob("")
    var INTER_ADD = InterHolderAdmob("")
    var INTER_SAVE_ADD = InterHolderAdmob("")
    var INTER_ITEMS_PLAYLIST = InterHolderAdmob("")
    var INTER_SELECT_CATEG_OR_CHANNEL = InterHolderAdmob("")
    var INTER_BACK_PLAY_TO_LIST = InterHolderAdmob("")

    var BANNER_SPLASH = ""
    var BANNER_HOME = ""
    var BANNER_DETAIL_PLAYLIST_CHANNEL = BannerHolderAdmob("")
    var BANNER_PLAY_CONTROL = ""

    var BANNER_COLLAP_PLAY_CONTROL = BannerHolderAdmob("")
    var BANNER_COLLAP_HOME = BannerHolderAdmob("")

    var NATIVE_LANGUAGE = NativeHolderAdmob("")
    var NATIVE_LANGUAGE_ID2 = NativeHolderAdmob("")
    var NATIVE_INTRO = NativeHolderAdmob("")
    var NATIVE_FULL_SCREEN_INTRO = NativeHolderAdmob("")
    var NATIVE_ADD = NativeHolderAdmob("")
    var NATIVE_PLAYLIST_CHANNEL = NativeHolderAdmob("")

    var ONRESUME = ""


    fun loadAndShowAdsNativeCustomHome(
        activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob, view: Int
    ) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.loadAndShowNativeAdsWithLayoutAds(activity,
            holder,
            viewGroup,
            view,
            GoogleENative.UNIFIED_BANNER,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    viewGroup.visible()
                }

                override fun onAdFail(error: String) {
                    viewGroup.gone()
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    postRevenueAdjust(adValue!!, adUnitAds)
                }

                override fun onClickAds() {

                }


            })
    }

    fun loadNativeFullScreen(
        context: Context, nativeHolder: NativeHolderAdmob
    ) {
        if (!AdmobUtils.isNetworkConnected(context) || isTestDevice) {
            IntroActivity.isIntroFullFail1 = true
            return
        }
        AdmobUtils.loadAndGetNativeFullScreenAds(context as Activity,
            nativeHolder,
            MediaAspectRatio.SQUARE,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onAdFail(error: String) {
                    IntroActivity.isIntroFullFail1 = true
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    adValue?.let { postRevenueAdjust(it, adUnitAds) }

                }

                override fun onClickAds() {

                }

                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {
                    checkAdsTest(ad)
                    IntroActivity.isIntroFullFail1 = false
                }

                override fun onNativeAdLoaded() {
                }
            })
    }    fun loadAndShowAdsNative(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.loadAndShowNativeAdsWithLayoutAds(activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            GoogleENative.UNIFIED_MEDIUM,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    viewGroup.visible()
                }

                override fun onAdFail(error: String) {
                    viewGroup.gone()
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    postRevenueAdjust(adValue!!, adUnitAds)
                }

                override fun onClickAds() {

                }


            })
    }


    fun loadAdsNative(context: Context, holder: NativeHolderAdmob) {
        AdmobUtils.loadAndGetNativeAds(context, holder, object : NativeAdmobCallback {
            override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

            }

            override fun onNativeAdLoaded() {

            }

            override fun onAdFail(error: String?) {

            }

            override fun onPaid(p0: AdValue?, p1: String?) {
                p0?.let { postRevenueAdjust(it, p1) }

            }

        })
    }

    interface onDelay {
        fun onDelay()
    }

    fun showAdsBannerSplash(
        activity: Activity,
        adsEnum: String,
        view: ViewGroup,
        line: View,
        onDelay: onDelay
    ) {
        if (AdmobUtils.isNetworkConnected(activity)) {
            AdmobUtils.loadAdBanner(activity, adsEnum, view, object : AdmobUtils.BannerCallBack {
                override fun onLoad() {
                    view.visibility = View.VISIBLE
                    line.visibility = View.VISIBLE
                    onDelay.onDelay()
                }

                override fun onClickAds() {

                }

                override fun onFailed(message: String) {
                    view.visibility = View.GONE
                    line.visibility = View.GONE
                    onDelay.onDelay()
                }

                override fun onPaid(adValue: AdValue?, mAdView: AdView?) {
                    if (mAdView != null) {
                        if (adValue != null) {
                            postRevenueAdjust(adValue, mAdView.adUnitId)
                        }
                    }

                }

            })
        } else {
            view.visibility = View.GONE
            line.visibility = View.GONE
        }
    }

    @JvmStatic
    fun showNativeCustom(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity) || isTestDevice) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeAdsWithLayout(activity,
            holder,
            viewGroup,
            R.layout.ad_template_small_bot,
            GoogleENative.UNIFIED_SMALL,
            object : AdmobUtils.AdsNativeCallBackAdmod {

                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }
                }


                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun showNativeFullScreen(
        context: Context, nativeHolder: NativeHolderAdmob, view: ViewGroup, onLoading: onLoading
    ) {
        if (!AdmobUtils.isNetworkConnected(context) || isTestDevice) {
            view.visibility = View.GONE
            return
        }
        if (IntroActivity.isIntroFullFail1) {
            view.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeFullScreenAdsWithLayout(context as Activity,
            nativeHolder,
            view,
            R.layout.ads_template_full_screen,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeFailed(massage: String) {
                    onLoading.onLoading()
                }

                override fun NativeLoaded() {
                    view.visibility = View.VISIBLE
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }

                }

            })
    }

    fun showAdInterSplash(
        activity: AppCompatActivity, interHolderAdmob: InterHolderAdmob, callback: AdListener
    ) {
        if (!AdmobUtils.isNetworkConnected(activity) || isTestDevice) {
            callback.onAdClosed()
            return
        }
        AppOpenManager.getInstance().isAppResumeEnabled = true
        AdmobUtils.loadAndShowAdInterstitial(activity, interHolderAdmob, object : AdsInterCallBack {
            override fun onStartAction() {
            }

            override fun onEventClickAdClosed() {
                callback.onAdClosed()
            }

            override fun onAdShowed() {
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler().postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (_: Exception) {

                    }
                }, 800)
            }

            override fun onAdLoaded() {

            }

            override fun onAdFail(p0: String?) {
                callback.onAdClosed()
            }

            override fun onClickAds() {
                TODO("Not yet implemented")
            }

            override fun onPaid(p0: AdValue?, p1: String?) {
                p0?.let { postRevenueAdjust(it, p1) }
            }

        }, true)
    }


    @JvmStatic
    fun showNativeWithLayout(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeAdsWithLayout(activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            GoogleENative.UNIFIED_MEDIUM,
            object : AdmobUtils.AdsNativeCallBackAdmod {

                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }

                }


                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun loadAndShowNativeFullScreen(
        context: Context,
        nativeHolder: NativeHolderAdmob,
        view: ViewGroup,
        layout: Int,
        onLoading: onLoading,
    ) {
        if (!AdmobUtils.isNetworkConnected(context) || isTestDevice) {
            IntroActivity.isIntroFullFail1 = true
            view.visibility = View.GONE
            onLoading.onLoading()
            return
        }
        AdmobUtils.loadAndShowNativeFullScreen(context as Activity,
            nativeHolder.ads,
            view,
            layout,
            MediaAspectRatio.SQUARE,
            object : NativeFullScreenCallBack {
                override fun onLoadFailed() {
                    onLoading.onLoading()


                }

                override fun onLoaded(nativeAd: NativeAd) {
                    checkAdsTest(ad = nativeAd)
                }
            })
    }

    interface onLoading {
        fun onLoading()
    }

    private fun checkAdsTest(ad: NativeAd?) {
        if (isTestDevice) {
            return
        }
        try {
            val testAdResponse = ad?.headline.toString().replace(" ", "").split(":")[0]
            Log.d("===Native", ad?.headline.toString().replace(" ", "").split(":")[0])
            val testAdResponses = arrayOf(
                "TestAd",
                "Anunciodeprueba",
                "Annoncetest",
                "테스트광고",
                "Annuncioditesto",
                "Testanzeige",
                "TesIklan",
                "Anúnciodeteste",
                "Тестовоеобъявление",
                "পরীক্ষামূলকবিজ্ঞাপন",
                "जाँचविज्ञापन",
                "إعلانتجريبي", "Quảngcáothửnghiệm"
            )
            isTestDevice = testAdResponses.contains(testAdResponse)
        } catch (_: Exception) {
            isTestDevice = true
            Log.d("===Native", "Error")
        }
    }


    fun showAdsBanner(activity: Activity, adsEnum: String, view: ViewGroup, line: View) {
        if (AdmobUtils.isNetworkConnected(activity)) {
            AdmobUtils.loadAdBanner(activity, adsEnum, view, object : AdmobUtils.BannerCallBack {
                override fun onLoad() {
                    view.visibility = View.VISIBLE
                    line.visibility = View.VISIBLE
                }

                override fun onClickAds() {

                }

                override fun onFailed(message: String) {
                    view.visibility = View.GONE
                    line.visibility = View.GONE
                }

                override fun onPaid(adValue: AdValue?, mAdView: AdView?) {
                    if (mAdView != null) {
                        if (adValue != null) {
                            postRevenueAdjust(adValue, mAdView.adUnitId)
                        }
                    }

                }

            })
        } else {
            view.visibility = View.GONE
            line.visibility = View.GONE
        }
    }

    fun loadAndShowNativeNoShimmer(
        activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob
    ) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.loadAndShowNativeAdsWithLayoutAdsNoShimmer(activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            GoogleENative.UNIFIED_MEDIUM,
            object : AdmobUtils.NativeAdCallbackNew {
                override fun onAdFail(error: String) {
                    viewGroup.visibility = View.GONE
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
                    adValue?.let { postRevenueAdjust(it, adUnitAds) }
                }

                override fun onClickAds() {

                }

                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                }
            })
    }


    fun showNativeBottom(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeAdsWithLayout(activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            GoogleENative.UNIFIED_MEDIUM,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }
                }

                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun showNativeSmall(activity: Activity, viewGroup: ViewGroup, holder: NativeHolderAdmob) {
        if (!AdmobUtils.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        AdmobUtils.showNativeAdsWithLayout(activity,
            holder,
            viewGroup,
            R.layout.ad_template_small_bot,
            GoogleENative.UNIFIED_SMALL,
            object : AdmobUtils.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }

                }

                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun loadInter(context: Context, interHolder: InterHolderAdmob) {
        AdmobUtils.loadAndGetAdInterstitial(context, interHolder, object : AdCallBackInterLoad {
            override fun onAdClosed() {
            }

            override fun onEventClickAdClosed() {
            }

            override fun onAdShowed() {
            }

            override fun onAdLoaded(
                interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd?,
                isLoading: Boolean
            ) {

            }

            override fun onAdFail(message: String?) {
            }

            override fun onPaid(p0: AdValue?, p1: String?) {
                if (p0 != null) {
                    postRevenueAdjust(p0, p1)
                }
            }
        })
    }

    fun showAdInter(
        context: Context, interHolder: InterHolderAdmob, callback: AdListener, event: String
    ) {
        logEvent(context, event + "_load")
        AppOpenManager.getInstance().isAppResumeEnabled = true
        AdmobUtils.showAdInterstitialWithCallbackNotLoadNew(
            context as Activity, interHolder, 10000, object : AdsInterCallBack {
                override fun onStartAction() {

                }

                override fun onEventClickAdClosed() {
                    logEvent(context, event + "_close")
                    callback.onAdClosed()
                    loadInter(context, interHolder)

                }

                override fun onAdShowed() {
                    logEvent(context, event + "_show")
                    AppOpenManager.getInstance().isAppResumeEnabled = false
                    Handler().postDelayed({
                        try {
                            AdmobUtils.dismissAdDialog()
                        } catch (e: Exception) {

                        }
                    }, 800)
                }

                override fun onAdLoaded() {

                }

                override fun onAdFail(error: String?) {
                    val log = error?.split(":")?.get(0)?.replace(" ", "_")
                    logEvent(context, event + "_fail")
                    log?.let { logEvent(context, event + "_" + it) }
                    callback.onAdClosed()
                    loadInter(context, interHolder)
                }

                override fun onClickAds() {

                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }
                }

            }, true
        )
    }

    fun showAdInterNotLoad(
        context: Context, interHolder: InterHolderAdmob, callback: AdListener, event: String
    ) {
        logEvent(context, event + "_load")
        AppOpenManager.getInstance().isAppResumeEnabled = true
        AdmobUtils.showAdInterstitialWithCallbackNotLoadNew(
            context as Activity, interHolder, 10000, object : AdsInterCallBack {
                override fun onStartAction() {

                }

                override fun onEventClickAdClosed() {
                    logEvent(context, event + "_close")
                    callback.onAdClosed()

                }

                override fun onAdShowed() {
                    logEvent(context, event + "_show")
                    AppOpenManager.getInstance().isAppResumeEnabled = false
                    Handler().postDelayed({
                        try {
                            AdmobUtils.dismissAdDialog()
                        } catch (e: Exception) {

                        }
                    }, 800)
                }

                override fun onAdLoaded() {

                }

                override fun onAdFail(error: String?) {
                    val log = error?.split(":")?.get(0)?.replace(" ", "_")
                    logEvent(context, event + "_fail")
                    log?.let { logEvent(context, event + "_" + it) }
                    callback.onAdClosed()
                }

                override fun onClickAds() {

                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }
                }

            }, true
        )
    }

    fun showAdBannerCollapsible(
        activity: Activity, adsEnum: BannerHolderAdmob, view: ViewGroup, line: View
    ) {
        if (AdmobUtils.isNetworkConnected(activity)) {
            AdmobUtils.loadAdBannerCollapsibleReload(activity,
                adsEnum,
                CollapsibleBanner.BOTTOM,
                view,
                object : AdmobUtils.BannerCollapsibleAdCallback {
                    override fun onBannerAdLoaded(adSize: AdSize) {
                        view.visibility = View.VISIBLE
                        line.visibility = View.VISIBLE

                        val params: ViewGroup.LayoutParams = view.layoutParams
                        params.height = adSize.getHeightInPixels(activity)
                        view.layoutParams = params
                    }

                    override fun onClickAds() {

                    }

                    override fun onAdFail(message: String) {
                        view.visibility = View.GONE
                        line.visibility = View.GONE
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                        postRevenueAdjust(adValue, mAdView.adUnitId)
                    }
                })
        } else {
            view.visibility = View.GONE
            line.visibility = View.GONE
        }
    }

    fun loadAndShowInterSplash(
        activity: Activity, interHolder: InterHolderAdmob, callback: AdListener
    ) {
        interHolder.inter = null
        AdmobUtils.isAdShowing = true
        AppOpenManager.getInstance().isAppResumeEnabled = true
        AdmobUtils.loadAndShowAdInterstitial(
            activity as AppCompatActivity, interHolder, object : AdsInterCallBack {
                override fun onStartAction() {

                }

                override fun onEventClickAdClosed() {
                    AdmobUtils.isAdShowing = false
                    callback.onAdClosed()
                }

                override fun onAdShowed() {
                    AppOpenManager.getInstance().isAppResumeEnabled = false

                }

                override fun onAdLoaded() {

                }

                override fun onAdFail(error: String?) {
                    AdmobUtils.isAdShowing = false
                    callback.onAdClosed()
                }

                override fun onClickAds() {

                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {
                    if (adValue != null) {
                        postRevenueAdjust(adValue, adUnitAds)
                    }
                }
            }, true
        )
    }

    private var isClick = true

    fun loadAndShowInter(
        activity: Activity,
        interHolder: InterHolderAdmob, onClose: () -> Unit
    ) {
        if(!AdmobUtils.isNetworkConnected(activity)){
            onClose.invoke()
            isClick =true
            return
        }
        isClick =false
        AdmobUtils.loadAndShowAdInterstitial(activity as AppCompatActivity, interHolder, object : AdsInterCallBack{
            override fun onStartAction() {

            }

            override fun onEventClickAdClosed() {
                onClose.invoke()
                isClick =true
            }

            override fun onAdShowed() {
                AppOpenManager.getInstance().isAppResumeEnabled = false
                Handler().postDelayed({
                    try {
                        AdmobUtils.dismissAdDialog()
                    } catch (e: Exception) {

                    }
                }, 800)
            }

            override fun onAdLoaded() {

            }

            override fun onAdFail(error: String?) {
                onClose.invoke()
                isClick =true
            }

            override fun onClickAds() {
                TODO("Not yet implemented")
            }

            override fun onPaid(adValue: AdValue?, adsId: String?) {
                if (adValue != null) {
                    postRevenueAdjust(adValue, adsId)
                }
            }
        }, true)

    }

    interface AdListener {
        fun onAdClosed()
    }


    fun View.gone() {
        visibility = View.GONE
    }

    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun logEvent(context: Context, eventName: String?) {
//        val firebaseAnalytics = FirebaseAnalytics.getInstance(context)
//        val bundle = Bundle()
//        bundle.putString("onEvent", context.javaClass.simpleName)
//        firebaseAnalytics.logEvent(
//            eventName + "_" + BuildConfig.VERSION_CODE,
//            bundle
//        )
//        Log.d("===Event", eventName + "_" + BuildConfig.VERSION_CODE)
    }

    fun postRevenueAdjust(ad: AdValue, adUnit: String?) {
        val adjustAdRevenue = AdjustAdRevenue("admob_sdk")
        val rev = ad.valueMicros.toDouble() / 1000000
        adjustAdRevenue.setRevenue(rev, ad.currencyCode)
        adjustAdRevenue.setAdRevenueUnit(adUnit)
        Adjust.trackAdRevenue(adjustAdRevenue)
    }
}