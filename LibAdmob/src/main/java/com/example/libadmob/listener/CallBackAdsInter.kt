package com.example.libadmob.listener

import com.google.android.gms.ads.AdValue

interface CallBackAdsInter {
    fun onStartAction()
    fun onEventClickAdClosed()
    fun onAdShowed()
    fun onAdLoaded()
    fun onAdFail(error: String?)
    fun onPaid(adValue: AdValue?, adUnitAds: String?)
}
