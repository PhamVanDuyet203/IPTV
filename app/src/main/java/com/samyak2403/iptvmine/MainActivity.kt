package com.samyak2403.iptvmine

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.samyak2403.iptvmine.screens.HomeFragment
import com.samyak2403.iptvmine.screens.LanguageSelectionActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPreferences = getSharedPreferences("AppSettings", MODE_PRIVATE)
        val selectedLanguage = sharedPreferences.getString("selectedLanguage", null)



        if (selectedLanguage == null) {
            val intent = Intent(this, LanguageSelectionActivity::class.java)
            startActivity(intent)
            finish()
        } else {

        }

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.commit {
                replace(R.id.fragmentContainerView, HomeFragment())
            }
        }

    }
    }