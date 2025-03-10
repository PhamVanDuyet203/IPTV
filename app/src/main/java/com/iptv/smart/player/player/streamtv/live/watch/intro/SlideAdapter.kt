package com.iptv.smart.player.player.streamtv.live.watch.intro

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter


class SlideAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int {
        return IntroActivity.numberPage
    }

    override fun createFragment(position: Int): Fragment {
        return IntroFragment.newInstance(position)
    }
}
