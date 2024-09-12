package com.example.libadmob.data.model

import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdView

class BannerConfigHolderModel(var ads: String) {
    var mAdView: AdView? = null
    val refreshHandler by lazy { Handler(Looper.getMainLooper()) }
}