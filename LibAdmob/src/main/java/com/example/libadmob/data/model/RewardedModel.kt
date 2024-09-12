package com.example.libadmob.data.model

import androidx.lifecycle.MutableLiveData
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd

open class RewardedModel(var ads: String) {
    var inter: RewardedInterstitialAd? = null
    val mutable: MutableLiveData<RewardedInterstitialAd> = MutableLiveData(null)
    var isLoading = false
}