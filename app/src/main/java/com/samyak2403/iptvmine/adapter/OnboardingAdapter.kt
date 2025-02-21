package com.samyak2403.iptvmine.adapter

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.screens.OnboardingFragment

class OnboardingAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {

    private val pages = listOf(
        OnboardingFragment.newInstance(R.drawable.onboard_img_1, fragmentActivity.getString(R.string.title_onboard_1), fragmentActivity.getString(R.string.content_onboard_1)),
        OnboardingFragment.newInstance(R.drawable.onboard_img_2, fragmentActivity.getString(R.string.title_onboard_2), fragmentActivity.getString(R.string.content_onboard_2)),
        OnboardingFragment.newInstance(R.drawable.onboard_img_3, fragmentActivity.getString(R.string.title_onboard_3), fragmentActivity.getString(R.string.content_onboard_3)),
        OnboardingFragment.newInstance(R.drawable.onboard_img_4, fragmentActivity.getString(R.string.title_onboard_4), fragmentActivity.getString(R.string.content_onboard_4))
    )

    override fun getItemCount(): Int = pages.size

    override fun createFragment(position: Int): Fragment = pages[position]
}
