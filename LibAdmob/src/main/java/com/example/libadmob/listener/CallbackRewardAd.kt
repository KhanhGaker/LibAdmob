package com.example.libadmob.listener

import com.google.android.gms.ads.AdValue

interface CallbackRewardAd {
    fun onAdClosed()
    fun onAdShowed()
    fun onAdFail(message: String?)
    fun onEarned()
    fun onPaid(adValue: AdValue?, adUnitAds: String?)
}
