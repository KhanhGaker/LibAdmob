package com.example.libadmob.ads.view

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.view.doOnLayout
import androidx.core.view.updateLayoutParams
import com.example.libadmob.data.model.BannerConfigHolderModel
import com.example.libadmob.listener.BannerRemoteConfig
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.OnPaidEventListener

@SuppressLint("ViewConstructor")
internal class BannerAdmobView(
    private val activity: Activity,
    adUnitId: String,
    private val bannerType: BannerAdmobPlugin.BannerType,
    refreshRateSec: Int?,
    private val cbFetchIntervalSec: Int,
    val bannerRemoteConfig: BannerRemoteConfig,
    bannerConfigHolderModel: BannerConfigHolderModel
) : BaseAdmobView(activity, refreshRateSec, bannerConfigHolderModel) {
    companion object {
        var lastCBRequestTime = 0L
    }

    private var hasSetAdSize = false

    init {
        bannerConfigHolderModel.mAdView = AdView(activity)
        bannerConfigHolderModel.mAdView?.adUnitId = adUnitId
        addView(bannerConfigHolderModel.mAdView, getCenteredLayoutParams(this))
    }

    private fun getCenteredLayoutParams(container: ViewGroup) = when (container) {
        is FrameLayout -> LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            this.gravity = Gravity.CENTER
        }

        is LinearLayout -> LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
            this.gravity = Gravity.CENTER
        }

        else -> LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }

    override fun loadAdInternal(onDone: () -> Unit) {
        if (!hasSetAdSize) {
            doOnLayout {
                try {
                    val adSize = getSizeAds(bannerType)
                    bannerConfigHolderModel.mAdView?.setAdSize(adSize)
                    bannerConfigHolderModel.mAdView?.updateLayoutParams {
                        width = adSize.getWidthInPixels(activity)
                        height = adSize.getHeightInPixels(activity)
                    }
                    hasSetAdSize = true
                    handlerLoadAds(onDone)
                } catch (_: Exception) {
                    Log.d("==BannerConfig==", "loadAdInternal: adSize error")
                }
            }
        } else {
            handlerLoadAds(onDone)
        }
    }


    private fun handlerLoadAds(onDone: () -> Unit) {
        var isCollapsibleBannerRequest = false
        val adRequestBuilder = AdRequest.Builder()
        when (bannerType) {
            BannerAdmobPlugin.BannerType.CollapsibleTop,
            BannerAdmobPlugin.BannerType.CollapsibleBottom -> {
                if (requestCollapse()) {
                    val position =
                        if (bannerType == BannerAdmobPlugin.BannerType.CollapsibleTop) "top" else "bottom"
                    adRequestBuilder.addNetworkExtrasBundle(
                        AdMobAdapter::class.java, Bundle().apply {
                            putString("collapsible", position)
                        }
                    )
                    isCollapsibleBannerRequest = true
                }
            }

            else -> {}
        }

        if (isCollapsibleBannerRequest) {
            lastCBRequestTime = System.currentTimeMillis()
        }
        bannerConfigHolderModel.mAdView?.onPaidEventListener = OnPaidEventListener { adValue ->
            bannerRemoteConfig.onAdPaid(
                adValue,
                bannerConfigHolderModel.mAdView!!
            )
        }
        bannerConfigHolderModel.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                bannerConfigHolderModel.mAdView?.adListener = object : AdListener() {}
                onDone()
                BannerAdmobPlugin.shimmerFrameLayout?.stopShimmer()
                bannerRemoteConfig.onBannerAdLoaded(getSizeAds(bannerType))

            }

            override fun onAdFailedToLoad(p0: LoadAdError) {
                bannerConfigHolderModel.mAdView?.adListener = object : AdListener() {}
                onDone()
                BannerAdmobPlugin.shimmerFrameLayout?.stopShimmer()
                bannerRemoteConfig.onAdFail()
            }
        }
        bannerConfigHolderModel.mAdView?.loadAd(adRequestBuilder.build())
    }

    private fun getSizeAds(bannerType: BannerAdmobPlugin.BannerType): AdSize {
        return when (bannerType) {
            BannerAdmobPlugin.BannerType.Standard -> AdSize.BANNER
            BannerAdmobPlugin.BannerType.Adaptive,
            BannerAdmobPlugin.BannerType.CollapsibleBottom,
            BannerAdmobPlugin.BannerType.CollapsibleTop -> {
                val displayMetrics = activity.resources.displayMetrics

                var adWidthPx = width.toFloat()
                if (adWidthPx == 0f) {
                    adWidthPx = displayMetrics.widthPixels.toFloat()
                }

                val density = displayMetrics.density
                val adWidth = (adWidthPx / density).toInt()

                AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        bannerConfigHolderModel.mAdView?.adListener = object : AdListener() {}
        bannerConfigHolderModel.mAdView?.destroy()
    }

    private fun requestCollapse(): Boolean {
        return System.currentTimeMillis() - lastCBRequestTime >= cbFetchIntervalSec * 1000L
    }

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) {
            bannerConfigHolderModel.mAdView?.resume()
        } else {
            bannerConfigHolderModel.mAdView?.pause()
        }
    }

}