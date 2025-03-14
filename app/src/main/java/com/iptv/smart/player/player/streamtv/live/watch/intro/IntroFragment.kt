package com.iptv.smart.player.player.streamtv.live.watch.intro

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.admob.max.dktlibrary.AdmobUtils
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.gone
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.visible
import com.iptv.smart.player.player.streamtv.live.watch.databinding.FragmentIntroBinding
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig


class IntroFragment : Fragment() {
    private val binding by lazy { FragmentIntroBinding.inflate(layoutInflater) }
    private lateinit var callbackIntro: CallbackIntro
    private var position = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    private fun showIntro1() {
        binding.imgNext.text = getString(R.string.next)
        showNativeIntro(0)
        binding.tvText.text = getString(R.string.title_onboard_1)
        binding.root.setBackgroundResource(R.drawable.onboard_img_1)
        binding.dot.setImageResource(R.drawable.dot1)
        binding.tvContent.text = getString(R.string.content_onboard_1)
    }

    private fun showIntro2() {
        binding.imgNext.text = getString(R.string.next)
        showNativeIntro(1)
        binding.tvText.text = getString(R.string.title_onboard_2)
        binding.root.setBackgroundResource(R.drawable.onboard_img_2)
        binding.dot.setImageResource(R.drawable.dot2)
        binding.tvContent.text = getString(R.string.content_onboard_2)
    }

    private fun showIntro3() {
        binding.imgNext.text = getString(R.string.next)
        showNativeIntro(2)
        binding.tvText.text = getString(R.string.title_onboard_3)
        binding.root.setBackgroundResource(R.drawable.onboard_img_3)
        binding.dot.setImageResource(R.drawable.dot3)
        binding.tvContent.text = getString(R.string.content_onboard_3)
    }

    private fun showIntro4() {
        binding.imgNext.text = getString(R.string.start)
        showNativeIntro(3)
        binding.tvText.text = getString(R.string.title_onboard_4)
        binding.root.setBackgroundResource(R.drawable.onboard_img_4)
        binding.dot.setImageResource(R.drawable.dot4)
        binding.tvContent.text = getString(R.string.content_onboard_4)
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        if (activity is CallbackIntro) callbackIntro = activity as CallbackIntro
        position = arguments?.getInt(ARG_POSITION) ?: 0
        binding.imgNext.setOnClickListener {
            callbackIntro.onNext(position)
        }
        if (arguments != null) {
            when (IntroActivity.numberPage) {
                4 -> {
                    fragmentPosition4()
                }

                5 -> {
                    if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "1" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition51()
                    } else if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "2" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition52()
                    } else if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "3" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition53()
                    }
                }

                6 -> {
                    if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "12" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition612()
                    } else if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "23" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition623()
                    } else if (RemoteConfig.NATIVE_FULL_SCREEN_INTRO_050325 == "13" && !IntroActivity.isIntroFullFail1) {
                        fragmentPosition613()
                    }
                }

                7 -> {
                    fragmentPosition7()
                }
            }
        }
    }

    private fun fragmentPosition4() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                showIntro1()
            }

            1 -> {
                showIntro2()
            }

            2 -> {
                showIntro3()
            }

            3 -> {
                showIntro4()
            }
        }
    }

    private fun fragmentPosition7() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                showIntro1()
                binding.lottieSlide.visible()
            }

            2 -> {
                showIntro2()
                binding.lottieSlide.gone()
            }

            4 -> {
                showIntro3()
                binding.lottieSlide.gone()
            }

            6 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }

            1 -> {
                AdsManager.showNativeFullScreen(
                    requireActivity(), AdsManager.NATIVE_FULL_SCREEN_INTRO, binding.frNativeFull,
                    object : AdsManager.onLoading {
                        override fun onLoading() {
                            binding.lottieSlide.visible()
                        }
                    }
                )
                showNativeFull()
            }

            3 -> {
                showNativeFull()
            }

            5 -> {
                showNativeFull()
            }
        }
    }

    private fun fragmentPosition612() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                showIntro1()
                binding.lottieSlide.visible()
            }

            2 -> {
                showIntro2()
            }

            4 -> {
                showIntro3()
            }

            1 -> {
                AdsManager.showNativeFullScreen(
                    requireActivity(), AdsManager.NATIVE_FULL_SCREEN_INTRO, binding.frNativeFull,
                    object : AdsManager.onLoading {
                        override fun onLoading() {
                            binding.lottieSlide.visible()
                        }
                    }
                )
                showNativeFull()
            }

            3 -> {
                showNativeFull()
            }

            5 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }

    private fun fragmentPosition613() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                showIntro1()
                binding.lottieSlide.visible()

            }

            2 -> {
                showIntro2()
            }

            4 -> {
                showNativeFull()
            }

            1 -> {
                AdsManager.showNativeFullScreen(
                    requireActivity(), AdsManager.NATIVE_FULL_SCREEN_INTRO, binding.frNativeFull,
                    object : AdsManager.onLoading {
                        override fun onLoading() {
                            binding.lottieSlide.visible()
                        }
                    }
                )
                showNativeFull()
            }

            3 -> {

                showIntro3()
            }

            5 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }

    private fun fragmentPosition623() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                binding.lottieSlide.visible()
                showIntro1()
            }

            2 -> {
                showNativeFull()
            }

            4 -> {
                showNativeFull()
            }

            1 -> {

                showIntro2()
            }

            3 -> {
                showIntro3()
            }

            5 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }


    private fun fragmentPosition52() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                binding.lottieSlide.visible()
                showIntro1()

            }

            1 -> {
                showIntro2()
            }

            2 -> {
                showNativeFull()
            }

            3 -> {
                showIntro3()
                binding.lottieSlide.gone()
            }

            4 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }


    private fun fragmentPosition51() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                showIntro1()
                binding.lottieSlide.visible()
            }

            1 -> {
                AdsManager.showNativeFullScreen(
                    requireActivity(), AdsManager.NATIVE_FULL_SCREEN_INTRO, binding.frNativeFull,
                    object : AdsManager.onLoading {
                        override fun onLoading() {
                            binding.lottieSlide.visible()
                        }
                    }
                )
                showNativeFull()
            }

            2 -> {
                showIntro2()
            }

            3 -> {
                showIntro3()
                binding.lottieSlide.gone()
            }

            4 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }

    private fun fragmentPosition53() {
        showView(true)
        binding.lottieSlide.gone()
        when (position) {
            0 -> {
                binding.lottieSlide.visible()
                showIntro1()
            }

            1 -> {
                showIntro2()
            }

            2 -> {
                showIntro3()
            }

            3 -> {
                showNativeFull()
            }

            4 -> {
                showIntro4()
                binding.lottieSlide.gone()
            }
        }
    }


    private fun showView(isShow: Boolean) {
        binding.apply {
            if (!isShow && AdmobUtils.isNetworkConnected(requireActivity())) {
                scrollView.gone()
                imgNext.gone()
                flNative.gone()
                frNativeFull.visible()
            } else {
                scrollView.visible()
                flNative.visible()
                imgNext.visible()
                frNativeFull.gone()
            }
        }
    }

    private fun showNativeFull() {
        binding.lottieSlide.gone()
        showView(false)
    }

    override fun onResume() {
        super.onResume()
        if (binding.frNativeFull.visibility == View.VISIBLE) {
            if (AdmobUtils.isNetworkConnected(requireContext())) {
                binding.lottieSlide.gone()
            } else {
                binding.lottieSlide.visible()
            }
        }

        if (position > 1) {
            RemoteConfig.setReload(requireActivity(), true)
        }
        if (binding.frNativeFull.visibility == View.VISIBLE && RemoteConfig.getReload(
                requireActivity()
            )
        ) {
            AdsManager.loadAndShowNativeFullScreen(
                requireActivity(),
                AdsManager.NATIVE_FULL_SCREEN_INTRO,
                binding.frNativeFull,
                R.layout.ads_template_full_screen,
                object : AdsManager.onLoading {
                    override fun onLoading() {
                        binding.lottieSlide.visible()
                    }

                })
        }
    }

    private fun showAds() {
        AdsManager.showNativeWithLayout(
            requireActivity(), binding.flNative, AdsManager.NATIVE_INTRO)
    }

    private fun showNativeIntro(position: Int) {
        if (AdsManager.isTestDevice) {
            binding.flNative.gone()
            return
        }
        when (position) {
            0 -> {
                if (RemoteConfig.NATIVE_INTRO_050325.contains("1")) {
                    binding.flNative.visible()
                    showAds()
                } else {
                    binding.flNative.gone()
                }
            }

            1 -> {
                if (RemoteConfig.NATIVE_INTRO_050325.contains("2")) {
                    binding.flNative.visible()
                    showAds()
                } else {
                    binding.flNative.gone()
                }
            }

            2 -> {
                if (RemoteConfig.NATIVE_INTRO_050325.contains("3")) {
                    binding.flNative.visible()
                    showAds()
                } else {
                    binding.flNative.gone()
                }
            }

            3 -> {
                if (RemoteConfig.NATIVE_INTRO_050325.contains("4")) {
                    binding.flNative.visible()
                    showAds()
                } else {
                    binding.flNative.gone()
                }
            }
        }

    }

    companion object {
        private const val ARG_POSITION = "position"
        fun newInstance(position: Int): IntroFragment {
            val fragment = IntroFragment()
            val args = Bundle()
            args.putInt(ARG_POSITION, position)
            fragment.arguments = args
            return fragment
        }
    }

    interface CallbackIntro {
        fun onNext(position: Int)
    }
}
