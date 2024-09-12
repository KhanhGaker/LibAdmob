package com.example.libadmob.listener

import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView

interface BannerRemoteConfig {
    fun onBannerAdLoaded(adSize: AdSize?)
    fun onAdFail()
    fun onAdPaid(adValue: AdValue, mAdView: AdView)
}