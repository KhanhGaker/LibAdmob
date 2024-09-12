package com.example.libadmob.ads.view

import android.app.Activity
import android.content.Context
import android.widget.FrameLayout
import androidx.annotation.CallSuper
import com.example.libadmob.ads.view.BannerAdmobPlugin.Companion.log
import com.example.libadmob.data.model.BannerConfigHolderModel
import com.example.libadmob.listener.BannerRemoteConfig
import kotlin.math.max

abstract class BaseAdmobView(
    context: Context,
    private val refreshRateSec: Int?, val bannerConfigHolderModel: BannerConfigHolderModel
) : FrameLayout(context) {

    private var nextRefreshTime = 0L

    private var isPausedOrDestroy = false

    fun loadAd() {
        log("LoadAd ...")
        nextRefreshTime = 0L // Not allow scheduling until ad request is done
        stopBannerRefreshScheduleIfNeed()

        loadAdInternal {
            log("On load ad done ...")
            calculateNextBannerRefresh()
            if (!isPausedOrDestroy) scheduleNextBannerRefreshIfNeed()
        }
    }

    protected abstract fun loadAdInternal(onDone: () -> Unit)

    @CallSuper
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (isVisible) onResume()
        else onPause()
    }

    @CallSuper
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    private fun onResume() {
        isPausedOrDestroy = false
        scheduleNextBannerRefreshIfNeed()
    }

    private fun onPause() {
        isPausedOrDestroy = true
        stopBannerRefreshScheduleIfNeed()
    }

    private fun onDestroy() {
        isPausedOrDestroy = true
        stopBannerRefreshScheduleIfNeed()
    }

    private fun calculateNextBannerRefresh() {
        if (refreshRateSec == null) return
        nextRefreshTime = System.currentTimeMillis() + refreshRateSec * 1000L
    }

    private fun scheduleNextBannerRefreshIfNeed() {
        if (refreshRateSec == null) return
        if (nextRefreshTime <= 0L) return

        val delay = max(0L, nextRefreshTime - System.currentTimeMillis())

        stopBannerRefreshScheduleIfNeed()
        //Check size FrameLayout
        log("Ads are scheduled to show in $delay mils")
        bannerConfigHolderModel.refreshHandler.postDelayed({ loadAd() }, delay)
    }

    private fun stopBannerRefreshScheduleIfNeed() {
        bannerConfigHolderModel.refreshHandler.removeCallbacksAndMessages(null)
    }

    internal object Factory {
        fun getAdView(
            activity: Activity,
            adUnitId: String,
            bannerType: BannerAdmobPlugin.BannerType,
            refreshRateSec: Int?,
            cbFetchIntervalSec: Int,
            bannerRemoteConfig: BannerRemoteConfig,
            bannerConfigHolderModel: BannerConfigHolderModel
        ): BaseAdmobView {
            return when (bannerType) {
                BannerAdmobPlugin.BannerType.Adaptive,
                BannerAdmobPlugin.BannerType.Standard,
                BannerAdmobPlugin.BannerType.CollapsibleBottom,
                BannerAdmobPlugin.BannerType.CollapsibleTop -> BannerAdmobView(
                    activity,
                    adUnitId,
                    bannerType,
                    refreshRateSec,
                    cbFetchIntervalSec,
                    bannerRemoteConfig,
                    bannerConfigHolderModel
                )
            }
        }
    }
}