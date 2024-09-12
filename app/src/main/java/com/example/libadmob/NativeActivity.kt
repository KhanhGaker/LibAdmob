package com.example.libadmob

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class NativeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_native)

        val native = findViewById<FrameLayout>(R.id.fr_native)
        ManagerAds.showNativeMedium(this,native, ManagerAds.NATIVE)
    }
}