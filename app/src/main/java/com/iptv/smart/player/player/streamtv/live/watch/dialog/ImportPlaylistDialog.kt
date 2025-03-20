package com.iptv.smart.player.player.streamtv.live.watch.dialog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager
import com.iptv.smart.player.player.streamtv.live.watch.ads.AdsManager.INTER_ADD
import com.iptv.smart.player.player.streamtv.live.watch.remoteconfig.RemoteConfig
import com.iptv.smart.player.player.streamtv.live.watch.screens.ActivityAddPlaylistFromDevice
import com.iptv.smart.player.player.streamtv.live.watch.screens.ActivityImportPlaylistM3U
import com.iptv.smart.player.player.streamtv.live.watch.screens.ActivityImportPlaylistUrl
import com.iptv.smart.player.player.streamtv.live.watch.screens.HowToUseActivity
import com.iptv.smart.player.player.streamtv.live.watch.utils.Common

class ImportPlaylistDialog : DialogFragment() {
    private var place = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {


        return inflater.inflate(R.layout.item_import_option, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        view.findViewById<TextView>(R.id.btnImportUrl).setOnClickListener {
            place = 1
            startAds()
        }

        view.findViewById<TextView>(R.id.btnImportM3U).setOnClickListener {
            place = 2
            startAds()
        }

        view.findViewById<TextView>(R.id.btnImportDevice).setOnClickListener {
            place = 3
            startAds()
        }
        view.findViewById<TextView>(R.id.btn_howtouse).setOnClickListener {
            openActivity(HowToUseActivity::class.java)
        }
    }

    private fun checkCLick() {
        when (place) {
            1 -> {
                openActivity(ActivityImportPlaylistUrl::class.java)
            }

            2 -> {
                openActivity(ActivityImportPlaylistM3U::class.java)
            }

            3 -> {
                openActivity(ActivityAddPlaylistFromDevice::class.java)
            }
        }
    }

    private fun startAds() {
        when (RemoteConfig.INTER_ADD_050325) {
            "0" -> {
                checkCLick()
            }

            else -> {
                Common.countInterAddOption++
                if (Common.countInterAddOption % RemoteConfig.INTER_ADD_050325.toInt() == 0) {
                    AdsManager.loadAndShowInter(requireActivity(), INTER_ADD) {
                        checkCLick()
                    }
                } else {
                    checkCLick()
                }
            }
        }

    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->

            val params = window.attributes
            params.gravity = Gravity.BOTTOM
            params.width = WindowManager.LayoutParams.MATCH_PARENT
            window.attributes = params


            window.setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    private fun openActivity(activityClass: Class<*>) {
        startActivity(Intent(requireContext(), activityClass))
        dismiss()
    }
}