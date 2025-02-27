package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.Language
import com.samyak2403.iptvmine.adapter.LanguageAdapter
import java.util.Locale

class LanguageSelectionActivity : AppCompatActivity() {

    private lateinit var checkIcon: ImageButton
    private lateinit var adapter: LanguageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_language_selector)

        val languageList = findViewById<RecyclerView>(R.id.language_list)
        checkIcon = findViewById(R.id.check_icon)

        languageList.layoutManager = LinearLayoutManager(this)
        val languages = getLanguages()

        adapter = LanguageAdapter(languages)
        languageList.adapter = adapter

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val savedLanguageCode = sharedPreferences.getString("selectedLanguage", null)
        if (savedLanguageCode != null) {
            val savedLanguage = languages.find { it.code == savedLanguageCode }
            savedLanguage?.let {
                val position = languages.indexOf(it)
                adapter.updateSelection(position)
            }
        }

        checkIcon.setOnClickListener {
            val selectedLanguage = adapter.getSelectedLanguage()
            if (selectedLanguage != null) {
                saveLanguageAndProceed(selectedLanguage.code)
            } else {
                Toast.makeText(this, "Please select a language first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLanguageAndProceed(languageCode: String) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("selectedLanguage", languageCode)
            apply()
        }

        applyLocale(languageCode)

        // Kiểm tra xem Onboarding đã hoàn thành chưa
        val isOnboardingCompleted = sharedPreferences.getBoolean("isOnboardingCompleted", false)
        val intent = if (isOnboardingCompleted) {
            Intent(this, HomePageActivity::class.java) // Nếu Onboarding đã hoàn thành, đi thẳng tới HomePage
        } else {
            Intent(this, OnboardingActivity::class.java) // Nếu chưa hoàn thành, đi tới Onboarding
        }

        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

    private fun applyLocale(languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        resources.updateConfiguration(config, resources.displayMetrics)
    }

    private fun getLanguages(): List<Language> {
        return listOf(
            Language(getString(R.string.english), "en", R.drawable.ic_english),
            Language(getString(R.string.korean), "ko", R.drawable.ic_korean),
            Language(getString(R.string.spanish), "es", R.drawable.ic_spanish),
            Language(getString(R.string.french), "fr", R.drawable.ic_french),
            Language(getString(R.string.arabic), "ar", R.drawable.ic_arabic),
            Language(getString(R.string.bengali), "bn", R.drawable.ic_bengali),
            Language(getString(R.string.russian), "ru", R.drawable.ic_russian),
            Language(getString(R.string.portuguese), "pt", R.drawable.ic_portuguese),
            Language(getString(R.string.indonesia), "in", R.drawable.ic_indonesian),
            Language(getString(R.string.hindi), "hi", R.drawable.ic_hindi),
            Language(getString(R.string.italian), "it", R.drawable.ic_italian),
            Language(getString(R.string.german), "de", R.drawable.ic_german)
        )
    }
}