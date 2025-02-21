package com.samyak2403.iptvmine.screens

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.OnboardingAdapter
import com.samyak2403.iptvmine.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {

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
                    else -> R.drawable.onboard_img_1 // Thêm giá trị mặc định nếu có
                }
                binding.viewPager.setBackgroundResource(backgroundRes)
            }
        })
    }
}
