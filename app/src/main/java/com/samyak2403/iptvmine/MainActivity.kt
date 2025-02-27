package com.samyak2403.iptvmine

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.samyak2403.iptvmine.screens.HomeFragment
import com.samyak2403.iptvmine.screens.LanguageSelectionActivity
import java.util.Locale

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Lấy ngôn ngữ đã lưu từ SharedPreferences trước khi gọi super.onCreate()
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selectedLanguage", null)

        // Áp dụng ngôn ngữ đã lưu (nếu có)
        if (selectedLanguage != null) {
            applyLocale(selectedLanguage)
        }

        super.onCreate(savedInstanceState)

        // Nếu chưa có ngôn ngữ nào được chọn, chuyển sang LanguageSelectionActivity
        if (selectedLanguage == null) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
            return // Thoát hàm để không tiếp tục xử lý giao diện MainActivity
        }

        // Nếu đã có ngôn ngữ, hiển thị giao diện chính
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, HomeFragment())
            }
        }
    }

    // Hàm áp dụng Locale
    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}