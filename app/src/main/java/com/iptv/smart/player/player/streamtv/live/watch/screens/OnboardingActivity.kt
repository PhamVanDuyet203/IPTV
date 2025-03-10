package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.adapter.OnboardingAdapter
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity
import com.iptv.smart.player.player.streamtv.live.watch.databinding.ActivityOnboardingBinding

class OnboardingActivity : BaseActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Thiết lập Adapter cho ViewPager2
        binding.viewPager.adapter = OnboardingAdapter(this)

        // Lắng nghe sự kiện thay đổi trang
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                super.onPageScrolled(position, positionOffset, positionOffsetPixels)

                // Đổi hình nền dựa vào trang hiện tại
                val backgroundRes = when (position) {
                    0 -> R.drawable.onboard_img_1
                    1 -> R.drawable.onboard_img_2
                    2 -> R.drawable.onboard_img_3
                    3 -> R.drawable.onboard_img_4
                    else -> R.drawable.onboard_img_1
                }
                binding.viewPager.setBackgroundResource(backgroundRes)
            }
        })
    }
}
