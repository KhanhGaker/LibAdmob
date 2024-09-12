package com.example.libadmob.ads.view

import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import com.example.libadmob.R
import com.example.libadmob.ads.InitializeAdmob
import com.example.libadmob.ads.view.BannerAdmobPlugin.BannerConfig.Companion.TYPE_ADAPTIVE
import com.example.libadmob.ads.view.BannerAdmobPlugin.BannerConfig.Companion.TYPE_COLLAPSIBLE_BOTTOM
import com.example.libadmob.ads.view.BannerAdmobPlugin.BannerConfig.Companion.TYPE_COLLAPSIBLE_TOP
import com.example.libadmob.ads.view.BannerAdmobPlugin.BannerConfig.Companion.TYPE_STANDARD
import com.example.libadmob.data.model.BannerConfigHolderModel
import com.example.libadmob.listener.BannerRemoteConfig
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.gson.annotations.SerializedName

@SuppressLint("ViewConstructor")
class BannerAdmobPlugin(
    private val activity: Activity,
    private val adContainer: ViewGroup,
    private val holder: BannerConfigHolderModel,
    private val bannerConfig: BannerConfig?,
    var bannerRemoteConfig: BannerRemoteConfig
) {
    companion object {

        var shimmerFrameLayout: ShimmerFrameLayout? = null
        private var LOG_ENABLED = true

        fun setLogEnabled(enabled: Boolean) {
            LOG_ENABLED = enabled
        }

        internal fun log(message: String) {
            if (LOG_ENABLED) {
                Log.d("BannerPlugin", message)
            }
        }
    }

    class Config {
        lateinit var defaultAdUnitId: String
        lateinit var defaultBannerType: BannerType
        var configKey: String? = null
        var defaultRefreshRateSec: Int? = null
        var defaultCBFetchIntervalSec: Int = 180
        var loadAdAfterInit = true
    }

    enum class BannerType {
        Standard,
        Adaptive,
        CollapsibleTop,
        CollapsibleBottom
    }

    private var adView: BaseAdmobView? = null
    var config: Config = Config().apply {
        this.defaultAdUnitId = holder.ads
        this.defaultBannerType = BannerType.Adaptive
        this.defaultRefreshRateSec = 10
        this.defaultCBFetchIntervalSec = 20
        this.loadAdAfterInit = InitializeAdmob.isShowAds
    }

    init {
        initViewAndConfig()
        if (config.loadAdAfterInit) {
            loadAd()
        }
    }

    private fun initViewAndConfig() {
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        adContainer.addView(tagView, 0)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        var adUnitId = config.defaultAdUnitId
        var bannerType = config.defaultBannerType
        var cbFetchIntervalSec = config.defaultCBFetchIntervalSec
        var refreshRateSec: Int? = config.defaultRefreshRateSec

        if (InitializeAdmob.isTesting) {
            adUnitId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        bannerType = when (bannerConfig?.type) {
            TYPE_STANDARD -> BannerType.Standard
            TYPE_ADAPTIVE -> BannerType.Adaptive
            TYPE_COLLAPSIBLE_TOP -> BannerType.CollapsibleTop
            TYPE_COLLAPSIBLE_BOTTOM -> BannerType.CollapsibleBottom
            else -> bannerType
        }
        refreshRateSec = bannerConfig?.refreshRateSec ?: refreshRateSec
        cbFetchIntervalSec = bannerConfig?.cbFetchIntervalSec ?: cbFetchIntervalSec

        log("\n adUnitId = $adUnitId \n bannerType = $bannerType \n refreshRateSec = $refreshRateSec \n cbFetchIntervalSec = $cbFetchIntervalSec")
        holder.mAdView?.destroy()
        adView = BaseAdmobView.Factory.getAdView(
            activity,
            adUnitId,
            bannerType,
            refreshRateSec,
            cbFetchIntervalSec,
            bannerRemoteConfig, holder
        )
        adContainer.addView(
            adView,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
        )
    }

    fun loadAd() {
        adView?.loadAd()
    }

    data class BannerConfig(
        @SerializedName("ad_unit_id")
        val adUnitId: String?,
        @SerializedName("type")
        val type: String?,
        @SerializedName("refresh_rate_sec")
        val refreshRateSec: Int?,
        @SerializedName("cb_fetch_interval_sec")
        val cbFetchIntervalSec: Int?
    ) {
        companion object {
            const val TYPE_STANDARD = "standard"
            const val TYPE_ADAPTIVE = "adaptive"
            const val TYPE_COLLAPSIBLE_TOP = "collapsible_top"
            const val TYPE_COLLAPSIBLE_BOTTOM = "collapsible_bottom"
        }
    }
}