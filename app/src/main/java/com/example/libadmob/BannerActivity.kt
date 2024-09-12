package com.example.libadmob

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BannerActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_banner)

        ManagerAds.showAdBanner(this,ManagerAds.BANNER,findViewById(R.id.fr_banner),
            findViewById(R.id.line))
    }
}