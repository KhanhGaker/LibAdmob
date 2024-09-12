package com.example.libadmob.listener

import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.nativead.NativeAd

interface CallbackNativeAd {
    fun onLoadedAndGetNativeAd(ad: NativeAd?)
    fun onNativeAdLoaded()
    fun onAdFail(error: String?)
    fun onAdPaid(adValue: AdValue?, adUnitAds: String?)
}
