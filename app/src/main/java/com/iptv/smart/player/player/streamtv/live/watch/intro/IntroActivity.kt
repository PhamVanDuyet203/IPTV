package com.iptv.smart.player.player.streamtv.live.watch.intro

import android.content.Intent
import android.os.Bundle
import com.admob.max.dktlibrary.AdmobUtils
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityIntroBinding
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.screens.HomePageActivity


class IntroActivity : BaseActivity(), IntroFragment.CallbackIntro {
    private lateinit var introViewPagerAdapter: SlideAdapter
    private val  binding by lazy { ActivityIntroBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        viewPager()
        binding.viewGone.setOnClickListener {

        }
    }


    private fun viewPager() {
        if (!AdmobUtils.isNetworkConnected(this)) {
            if (!isIntroFullFail1) {
                isIntroFullFail1 = true
            }
        }
        numberPage = if (!AdsManager.isTestDevice) {
            if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "123" && !isIntroFullFail1) {
                7
            } else if ((RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.length == 2) && !isIntroFullFail1) {
                6
            } else if ((RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.length == 1 && RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 != "0") && !isIntroFullFail1) {
                5
            } else {
                4
            }
        } else {
            4
        }

        introViewPagerAdapter = SlideAdapter(this)
        binding.screenViewpager.adapter = introViewPagerAdapter
        binding.screenViewpager.setOffscreenPageLimit(numberPage)

    }

    override fun onBackPressed() {
        if (binding.screenViewpager.currentItem < 1) {
            super.onBackPressed()
        } else {
            binding.screenViewpager.currentItem--
        }
    }


    override fun onStop() {
        super.onStop()
        binding.viewGone.gone()
    }

    override fun onNext(position: Int) {
        if (position < numberPage - 1) {
            when (position + 1) {
                1 -> {
                    if (RemoteConfig.NATIVE_INTRO_050325 == "1") {
                        showInterstitialAd {
                            binding.screenViewpager.currentItem++
                            enableUserInteraction()
                        }
                    } else {
                        binding.screenViewpager.currentItem++
                    }
                }

                2 -> {
                    if (RemoteConfig.NATIVE_INTRO_050325 == "2") {
                        showInterstitialAd {
                            binding.screenViewpager.currentItem++
                            enableUserInteraction()
                        }
                    } else {
                        binding.screenViewpager.currentItem++
                    }
                }

                3 -> {
                    if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325.contains("2") && !isIntroFullFail1) {
                        if (RemoteConfig.NATIVE_INTRO_050325 == "2") {
                            showInterstitialAd {
                                binding.screenViewpager.currentItem++
                                enableUserInteraction()
                            }
                        } else {
                            binding.screenViewpager.currentItem++
                        }
                    } else {
                        if (RemoteConfig.NATIVE_INTRO_050325 == "3") {
                            showInterstitialAd {
                                binding.screenViewpager.currentItem++
                                enableUserInteraction()
                            }
                        } else {
                            binding.screenViewpager.currentItem++
                        }
                    }
                }

                4 -> {
                    if (RemoteConfig.NATIVE_INTRO_050325 == "3") {
                        showInterstitialAd {
                            binding.screenViewpager.currentItem++
                            enableUserInteraction()
                        }
                    } else {
                        binding.screenViewpager.currentItem++
                    }
                }

                5 -> {
                    if (RemoteConfig.NATIVE_INTRO_050325 == "3") {
                        showInterstitialAd {
                            binding.screenViewpager.currentItem++
                            enableUserInteraction()
                        }
                    } else {
                        binding.screenViewpager.currentItem++
                    }
                }

            }
        } else {
            if (RemoteConfig.NATIVE_INTRO_050325 == "4") {
                showInterstitialAd {
                    startAc()
                    enableUserInteraction()
                }
            } else {
                startAc()
            }
        }
    }

    private fun showInterstitialAd(onAdClosed: (() -> Unit)? = null) {
        binding.viewGone.visible()
        AdsManager.showAdInterSplash(
            this,
            AdsManager.INTER_SPLASH,
            object : AdsManager.AdListener {
                override fun onAdClosed() {
                    onAdClosed?.invoke()
                }

            }
        )
    }


    private fun enableUserInteraction() {
        binding.viewGone.gone()
    }

    private fun startAc() {
        val intent = Intent(this@IntroActivity, HomePageActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    companion object {
        var isIntroFullFail1: Boolean = true
        var numberPage = 4
    }

}
