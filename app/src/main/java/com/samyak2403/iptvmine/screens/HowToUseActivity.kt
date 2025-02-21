package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.samyak2403.iptvmine.R

class HowToUseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_howtouse) // Gán layout cho Activity

        // Tìm ImageButton
        val closeButton = findViewById<ImageButton>(R.id.close_htu)

        // Xử lý sự kiện click
        closeButton.setOnClickListener {
            val intent = Intent(this, HomePageActivity::class.java)
            startActivity(intent)
            finish() // Đóng HowToUseActivity để không quay lại
        }
    }
}
