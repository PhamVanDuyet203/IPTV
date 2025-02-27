package com.samyak2403.iptvmine

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.samyak2403.iptvmine.screens.HomePageActivity
import com.samyak2403.iptvmine.screens.LanguageSelectionActivity
import com.samyak2403.iptvmine.screens.OnboardingActivity
import java.util.Locale

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selectedLanguage", null)
        val isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false)

        Handler(Looper.getMainLooper()).postDelayed({
            if (selectedLanguage == null) {
                // Chưa chọn ngôn ngữ
                val intent = Intent(this, LanguageSelectionActivity::class.java)
                startActivity(intent)
            } else {
                // Đã có ngôn ngữ
                applyLocale(selectedLanguage)
                if (isOnboardingCompleted) {
                    // Đã hoàn thành Onboarding, đi thẳng tới HomePage
                    val intent = Intent(this, HomePageActivity::class.java)
                    startActivity(intent)
                } else {
                    // Chưa hoàn thành Onboarding
                    val intent = Intent(this, OnboardingActivity::class.java)
                    startActivity(intent)
                }
            }
            finish()
        }, 3000)
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}