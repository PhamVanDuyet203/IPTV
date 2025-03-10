package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.tbuonomo.viewpagerdotsindicator.WormDotsIndicator

class OnboardingFragment : Fragment() {

    companion object {
        fun newInstance(imageRes: Int, title: String, content: String): OnboardingFragment {
            val fragment = OnboardingFragment()
            val args = Bundle()
            args.putInt("imageRes", imageRes)
            args.putString("title", title)
            args.putString("content", content)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.onboarding_vp, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val imageRes = arguments?.getInt("imageRes") ?: R.drawable.onboard_img_1
        val title = arguments?.getString("title") ?: ""
        val content = arguments?.getString("content") ?: ""

        view.findViewById<ImageView>(R.id.image_background).setImageResource(imageRes)
        view.findViewById<TextView>(R.id.text_title).text = title
        view.findViewById<TextView>(R.id.text_content).text = content

        // Gắn WormDotsIndicator với ViewPager2 từ Activity
        val dotsIndicator = view.findViewById<WormDotsIndicator>(R.id.worm_dots_indicator)
        val viewPager = requireActivity().findViewById<ViewPager2>(R.id.view_pager)
        val nextButton = view.findViewById<TextView>(R.id.next_text) // Text "Next" hoặc "Start"

        dotsIndicator.attachTo(viewPager)

        // Lắng nghe sự kiện thay đổi trang
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                val lastPage = viewPager.adapter?.itemCount?.minus(1) ?: 0
                nextButton.text = if (position == lastPage) getString(R.string.start) else getString(
                    R.string.next_onboarding)
            }
        })


        // Xử lý khi bấm "Next" hoặc "Start"
        nextButton.setOnClickListener {
            if (nextButton.text == getString(R.string.start)) {
                startActivity(Intent(requireActivity(), HomePageActivity::class.java))
                requireActivity().finish() // Đóng màn hình Onboarding
            } else {
                viewPager.currentItem += 1 // Chuyển sang trang tiếp theo
            }
        }
    }
}
