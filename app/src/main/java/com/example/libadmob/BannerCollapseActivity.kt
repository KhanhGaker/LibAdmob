package com.example.libadmob

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class BannerCollapseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_banner_collapse)

        ManagerAds.showAdBannerCollapsible(this,ManagerAds.BANNER_COLLAPSE,findViewById(R.id.fr_banner),
            findViewById(R.id.line))
    }
}