package com.iptv.smart.player.player.streamtv.live.watch.screens

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.iptv.smart.player.player.streamtv.live.watch.R
import com.iptv.smart.player.player.streamtv.live.watch.base.BaseActivity

class HowToUseActivity : BaseActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_howtouse)

        val closeButton = findViewById<ImageButton>(R.id.close_htu)
        val htuText = findViewById<TextView>(R.id.txt_htu)
        htuText.isSelected=true

        closeButton.setOnClickListener {
            finish()
        }
    }
}
