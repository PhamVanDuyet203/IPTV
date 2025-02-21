package com.samyak2403.iptvmine.screens

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Toast
import com.samyak2403.iptvmine.MainActivity
import com.samyak2403.iptvmine.R
import com.samyak2403.iptvmine.adapter.Language
import com.samyak2403.iptvmine.adapter.LanguageAdapter


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

        adapter = LanguageAdapter(languages) {}
        languageList.adapter = adapter

        // Khi bấm dấu tích, lưu ngôn ngữ và restart
        checkIcon.setOnClickListener {
            val selectedLanguage = adapter.getSelectedLanguage()
            if (selectedLanguage != null) {
                saveLanguageAndRestart(selectedLanguage.code)
            } else {
                Toast.makeText(this, "Please select a language first!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLanguageAndRestart(languageCode: String) {
        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString("selectedLanguage", languageCode)
            apply()
        }


        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }


    private fun getLanguages(): List<Language> {
        return listOf(
            Language("English", "en", R.drawable.ic_english),
            Language("Korean", "kr", R.drawable.ic_korean),
            Language("Spanish", "es", R.drawable.ic_spanish),
            Language("French", "fr", R.drawable.ic_french),
            Language("Arabic", "ar", R.drawable.ic_arabic),
            Language("Bengali", "bn", R.drawable.ic_bengali),
            Language("Russian", "ru", R.drawable.ic_russian),
            Language("Portuguese", "pt", R.drawable.ic_portuguese),
            Language("Indonesia", "in", R.drawable.ic_indonesian),
            Language("Hindi", "hi", R.drawable.ic_hindi),
            Language("Italian", "it", R.drawable.ic_italian),
            Language("German", "hi", R.drawable.ic_german)
        )
    }
}

