package com.example.libadmob.listener

import com.google.android.gms.ads.interstitial.InterstitialAd

interface CallBackLoadInter {
    fun onAdClosed()
    fun onEventClickAdClosed()
    fun onAdShowed()
    fun onAdLoaded(interstitialAd: InterstitialAd?, isLoading: Boolean)
    fun onAdFail(message: String?)
}
