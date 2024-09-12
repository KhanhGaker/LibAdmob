package com.example.libadmob.ads

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.airbnb.lottie.LottieAnimationView
import com.example.libadmob.R
import com.example.libadmob.ads.native_admob.HandlerNative.Companion.populateNativeAdView
import com.example.libadmob.ads.native_admob.HandlerNative.Companion.populateNativeAdViewNoBtn
import com.example.libadmob.ads.view.BannerAdmobPlugin
import com.example.libadmob.data.enum_model.EnumBannerCollapsible
import com.example.libadmob.data.enum_model.SizeNative
import com.example.libadmob.data.model.BannerConfigHolderModel
import com.example.libadmob.data.model.BannerModel
import com.example.libadmob.data.model.InterModel
import com.example.libadmob.data.model.NativeModel
import com.example.libadmob.data.model.RewardedModel
import com.example.libadmob.listener.BannerRemoteConfig
import com.example.libadmob.listener.CallBackAdsInter
import com.example.libadmob.listener.CallBackLoadInter
import com.example.libadmob.listener.CallbackAdLoad
import com.example.libadmob.listener.CallbackNativeAd
import com.example.libadmob.listener.CallbackRewardAd
import com.example.libadmob.utils.DialogShow
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnPaidEventListener
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Date
import java.util.Locale

object InitializeAdmob {
    //Dialog loading
    @JvmField
    var dialog: DialogShow? = null
    var dialogFullScreen: Dialog? = null
    @JvmField
    var isAdShowing = false
    var isClick = false

    var lastTimeShowInterstitial: Long = 0
    var timeOut = 0

    //Ẩn hiện quảng cáo
    @JvmField
    var isShowAds = true

    //Dùng ID Test để hiển thị quảng cáo
    @JvmField
    var isTesting = false



    //Reward Ads
    @JvmField
    var mRewardedAd: RewardedAd? = null
    var mInterstitialAd: InterstitialAd? = null
    var shimmerFrameLayout: ShimmerFrameLayout? = null
    //List device test
    var testDevices: MutableList<String> = ArrayList()
    var deviceId = ""
    //id thật
    var idIntersitialReal: String? = null

    //Hàm Khởi tạo admob
    @JvmStatic
    fun initAdmob(context: Context?, timeout: Int, isDebug: Boolean, isEnableAds: Boolean) {
        timeOut = timeout
        if (timeOut < 5000 && timeout != 0) {
            Toast.makeText(context, "Nên để limit time ~10000", Toast.LENGTH_LONG).show()
        }
        timeOut = if (timeout > 0) {
            timeout
        } else {
            10000
        }
        isTesting = isDebug
        isShowAds = isEnableAds
        MobileAds.initialize(context!!) { initializationStatus: InitializationStatus? -> }
        initListIdTest()
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(testDevices)
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)
        initAdRequest(timeout)
    }

    var adRequest: AdRequest? = null

    // get AdRequest
    @JvmStatic
    fun initAdRequest(timeOut: Int) {
        adRequest = AdRequest.Builder()
            .setHttpTimeoutMillis(timeOut)
            .build()
    }

    fun initListIdTest() {
        testDevices.add("D4A597237D12FDEC52BE6B2F15508BB")
    }

    //check open network
    @JvmStatic
    fun isNetworkConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo != null && cm.activeNetworkInfo!!.isConnected
    }

    @JvmStatic
    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = connectivityManager.activeNetwork ?: return false
        val actNw = connectivityManager.getNetworkCapabilities(nw) ?: return false
        return when {
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            //for other device how are able to connect with Ethernet
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            //for check internet over Bluetooth
            actNw.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH) -> true
            else -> false
        }
    }

    interface BannerCallBack {
        fun onClickAds()
        fun onLoad()
        fun onFailed(message: String)
        fun onPaid(adValue: AdValue?, mAdView: AdView?)
    }

    @JvmStatic
    fun loadAdBanner(
        activity: Activity,
        bannerId: String?,
        viewGroup: ViewGroup,
        bannerAdCallback: BannerCallBack
    ) {
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getAdSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue -> bannerAdCallback.onPaid(adValue, mAdView) }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onLoad()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                bannerAdCallback.onClickAds()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }
        if (adRequest != null) {
            mAdView.loadAd(adRequest!!)
        }
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAdBannerWithSize(
        activity: Activity,
        bannerId: String?,
        viewGroup: ViewGroup, adSize: AdSize,
        bannerAdCallback: BannerCallBack
    ) {
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            bannerAdCallback.onFailed("None Show")
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_id)
        }
        mAdView.adUnitId = bannerId!!
        var size = adSize
        if (size == AdSize.BANNER) {
            size = getAdSize(activity)
        }
        mAdView.setAdSize(size)
        val tagView = when (size) {
            AdSize.MEDIUM_RECTANGLE -> {
                activity.layoutInflater.inflate(R.layout.layout_banner_medium_loading, null, false)
            }

            AdSize.LARGE_BANNER -> {
                activity.layoutInflater.inflate(R.layout.layout_banner_large_loading, null, false)
            }

            else -> {
                activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
            }
        }

        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()
        mAdView.onPaidEventListener =
            OnPaidEventListener { adValue -> bannerAdCallback.onPaid(adValue, mAdView) }
        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onLoad()
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                bannerAdCallback.onFailed(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                bannerAdCallback.onClickAds()
            }

            override fun onAdClosed() {
                // Code to be executed when the user is about to return
                // to the app after tapping on an ad.
            }
        }
        if (adRequest != null) {
            mAdView.loadAd(adRequest!!)
        }
        Log.e(" Admod", "loadAdBanner")
    }

    interface BannerCollapsibleAdCallback {
        fun onClickAds()
        fun onBannerAdLoaded(adSize: AdSize)
        fun onAdFail(message: String)
        fun onAdPaid(adValue: AdValue, mAdView: AdView)
    }

    @JvmStatic
    fun loadAdBannerCollapsibleReload(
        activity: Activity,
        banner: BannerModel,
        enumBannerCollapsibleSize: EnumBannerCollapsible,
        viewGroup: ViewGroup,
        callback: BannerCollapsibleAdCallback
    ) {
        var bannerId = banner.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        try {
            banner.mAdView?.destroy()
        } catch (_: Exception) {

        }
        banner.mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        banner.mAdView?.adUnitId = bannerId
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(banner.mAdView, 1)
        } catch (_: Exception) {

        }
        val adSize = getAdSize(activity)
        banner.mAdView?.setAdSize(adSize)
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        banner.mAdView?.adListener = object : AdListener() {
            override fun onAdLoaded() {
                banner.mAdView?.onPaidEventListener =
                    OnPaidEventListener { adValue -> callback.onAdPaid(adValue, banner.mAdView!!) }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerAdLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onAdFail(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onClickAds()
            }

            override fun onAdClosed() {

            }
        }
        val extras = Bundle()
        var anchored = "top"
        anchored = if (enumBannerCollapsibleSize === EnumBannerCollapsible.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        banner.mAdView?.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAdBannerCollapsible(
        activity: Activity,
        bannerId: String?,
        enumBannerCollapsibleSize: EnumBannerCollapsible,
        viewGroup: ViewGroup,
        callback: BannerCollapsibleAdCallback
    ) {
        var bannerId = bannerId
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        val mAdView = AdView(activity)
        if (isTesting) {
            bannerId = activity.getString(R.string.test_ads_admob_banner_collapsible_id)
        }
        mAdView.adUnitId = bannerId!!
        val adSize = getAdSize(activity)
        mAdView.setAdSize(adSize)
        val tagView = activity.layoutInflater.inflate(R.layout.layoutbanner_loading, null, false)
        try {
            viewGroup.removeAllViews()
            viewGroup.addView(tagView, 0)
            viewGroup.addView(mAdView, 1)
        } catch (_: Exception) {

        }
        shimmerFrameLayout = tagView.findViewById(R.id.shimmer_view_container)
        shimmerFrameLayout?.startShimmer()

        mAdView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                mAdView.onPaidEventListener =
                    OnPaidEventListener { adValue -> callback.onAdPaid(adValue, mAdView) }
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onBannerAdLoaded(adSize)
            }

            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(" Admod", "failloadbanner" + adError.message)
                shimmerFrameLayout?.stopShimmer()
                viewGroup.removeView(tagView)
                callback.onAdFail(adError.message)
            }

            override fun onAdOpened() {}
            override fun onAdClicked() {
                callback.onClickAds()
            }

            override fun onAdClosed() {

            }
        }
        val extras = Bundle()
        var anchored = "top"
        anchored = if (enumBannerCollapsibleSize === EnumBannerCollapsible.TOP) {
            "top"
        } else {
            "bottom"
        }
        extras.putString("collapsible", anchored)
        val adRequest2 =
            AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
        mAdView.loadAd(adRequest2)
        Log.e(" Admod", "loadAdBanner")
    }

    @JvmStatic
    fun loadAndShowBannerCollapsibleWithConfig(
        activity: Activity,
        id: BannerConfigHolderModel, refreshRateSec: Int, cbFetchIntervalSec: Int, view: ViewGroup,
        bannerAdCallback: BannerCollapsibleAdCallback
    ) {
        var bannerAdmobPlugin: BannerAdmobPlugin? = null
        val bannerConfig = BannerAdmobPlugin.BannerConfig(
            id.ads,
            "collapsible_bottom",
            refreshRateSec,
            cbFetchIntervalSec
        )
        bannerAdmobPlugin = bannerConfig.adUnitId?.let {
            BannerAdmobPlugin(
                activity, view, id, bannerConfig, object : BannerRemoteConfig {
                    override fun onBannerAdLoaded(adSize: AdSize?) {
                        adSize?.let { it1 -> bannerAdCallback.onBannerAdLoaded(it1) }
                    }

                    override fun onAdFail() {
                        Log.d("===Banner", "Banner2")
                        bannerAdCallback.onAdFail("Banner Failed")
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {
                        bannerAdCallback.onAdPaid(adValue, mAdView)
                    }
                })
        }
    }

    private fun getAdSize(context: Activity): AdSize {
        // Step 2 - Determine the screen width (less decorations) to use for the ad width.
        val display = context.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)
        val widthPixels = outMetrics.widthPixels.toFloat()
        val density = outMetrics.density
        val adWidth = (widthPixels / density).toInt()
        // Step 3 - Get adaptive ad size and return for setting on the ad view.
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, adWidth)
    }

    //Load native 1 in here
    @JvmStatic
    fun loadAndGetNativeAds(
        context: Context,
        nativeModel: NativeModel,
        adCallback: CallbackNativeAd
    ) {
        if (!isShowAds || !isNetworkConnected(context)) {
            adCallback.onAdFail("No internet")
            return
        }
        //If native is loaded return
        if (nativeModel.nativeAd != null) {
            Log.d("===AdsLoadsNative", "Native not null")
            return
        }
        if (isTesting) {
            nativeModel.ads = context.getString(R.string.test_ads_admob_native_id)
        }
        nativeModel.isLoad = true
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader = AdLoader.Builder(context, nativeModel.ads)
            .forNativeAd { nativeAd ->
                nativeModel.nativeAd = nativeAd
                nativeModel.isLoad = false
                nativeModel.native_mutable.value = nativeAd
                nativeAd.setOnPaidEventListener { adValue: AdValue? ->
                    adValue?.let {
                        adCallback.onAdPaid(
                            it, nativeModel.ads
                        )
                    }
                }
                adCallback.onLoadedAndGetNativeAd(nativeAd)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    nativeModel.nativeAd = null
                    nativeModel.isLoad = false
                    nativeModel.native_mutable.value = null
                    adCallback.onAdFail(adError.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
    }

    //Load native 2 in here
    interface AdsNativeCallBackAdmod {
        fun NativeLoaded()
        fun NativeFailed(massage: String)
        fun onPaidNative(adValue: AdValue, adUnitAds: String)
    }

    @JvmStatic
    fun showNativeAdsWithLayout(
        activity: Activity,
        nativeModel: NativeModel,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        callback: AdsNativeCallBackAdmod
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        viewGroup.removeAllViews()
        if (!nativeModel.isLoad) {
            if (nativeModel.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeModel.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === SizeNative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            }
            viewGroup.addView(tagView, 0)
            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeModel.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        callback.onPaidNative(it, nativeModel.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdView(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                    callback.NativeLoaded()
                    nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }

    // ads native
    @JvmStatic
    fun loadAndShowNativeAdsWithLayout(
        activity: Activity,
        s: String?,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        adCallback: CallbackNativeAd
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = s
        val tagView: View = if (size === SizeNative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        viewGroup.addView(tagView, 0)
        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader = AdLoader.Builder(activity, s!!)
            .forNativeAd { nativeAd ->
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(
                        adValue,
                        s
                    )
                }
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    viewGroup.removeAllViews()
                    adCallback.onAdFail(adError.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutShimmer(
        activity: Activity,
        s: String?,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        adCallback: CallbackNativeAd
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = s
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val videoOptions =
            VideoOptions.Builder().setStartMuted(false).build()
        val adLoader: AdLoader = AdLoader.Builder(activity, s!!)
            .forNativeAd { nativeAd ->
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(
                        adValue,
                        s
                    )
                }
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    viewGroup.removeAllViews()
                    adCallback.onAdFail(adError.message)
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    interface NativeAdCallbackNew {
        fun onLoadedAndGetNativeAd(ad: NativeAd?)
        fun onNativeAdLoaded()
        fun onAdFail(error: String)
        fun onAdPaid(adValue: AdValue?, adUnitAds: String?)
        fun onClickAds()

    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAds(
        activity: Activity,
        nativeModel: NativeModel,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        adCallback: NativeAdCallbackNew
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
//        val videoOptions =
//            VideoOptions.Builder().setStartMuted(false).build()
        viewGroup.removeAllViews()
        var s = nativeModel.ads
        val tagView: View = if (size === SizeNative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        viewGroup.addView(tagView, 0)
        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    viewGroup.removeAllViews()
                    nativeModel.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsNoShimmer(
        activity: Activity,
        nativeModel: NativeModel,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        adCallback: NativeAdCallbackNew
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        var s = nativeModel.ads
        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdView(nativeAd, adView, size)
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    viewGroup.removeAllViews()
                    nativeModel.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    //Load Inter in here
    @JvmStatic
    fun loadAndGetAdInterstitial(
        activity: Context,
        interModel: InterModel,
        adLoadCallback: CallBackLoadInter
    ) {
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adLoadCallback.onAdFail("None Show")
            return
        }
        if (interModel.inter != null) {
            Log.d("===AdsInter", "inter not null")
            return
        }
        interModel.check = true
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (isTesting) {
            interModel.ads = activity.getString(R.string.test_ads_admob_inter_id)
        }
        idIntersitialReal = interModel.ads
        InterstitialAd.load(
            activity,
            idIntersitialReal!!,
            adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    if (isClick) {
                        interModel.mutable.value = interstitialAd
                    }
                    interModel.inter = interstitialAd
                    interModel.check = false
                    adLoadCallback.onAdLoaded(interstitialAd, false)
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    isAdShowing = false
                    if (mInterstitialAd != null) {
                        mInterstitialAd = null
                    }
                    interModel.check = false
                    if (isClick) {
                        interModel.mutable.value = null
                    }
                    adLoadCallback.onAdFail(loadAdError.message)
                }
            })
    }

    //Load Inter 2 in here if inter 1 false

    //Show Inter in here
    @JvmStatic
    fun showAdInterstitialWithCallbackNotLoadNew(
        activity: Activity,
        interModel: InterModel,
        timeout: Long,
        adCallback: CallBackAdsInter?,
        enableLoadingDialog: Boolean
    ) {
        isClick = true
        //Check internet
        if (!isShowAds || !isNetworkConnected(activity)) {
            isAdShowing = false
            if (AdsOnResume.instance!!.isInitialized) {
                AdsOnResume.instance!!.isAppResumeEnabled = true
            }
            adCallback?.onAdFail("No internet")
            return
        }
        adCallback?.onAdLoaded()
        val handler = Handler(Looper.getMainLooper())
        //Check timeout show inter
        val runnable = Runnable {
            if (interModel.check) {
                if (AdsOnResume.instance!!.isInitialized) {
                    AdsOnResume.instance!!.isAppResumeEnabled = true
                }
                isClick = false
                interModel.mutable.removeObservers((activity as LifecycleOwner))
                isAdShowing = false
                dismissAdDialog()
                adCallback?.onAdFail("timeout")
            }
        }
        handler.postDelayed(runnable, timeout)
        //Inter is Loading...
        if (interModel.check) {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            interModel.mutable.observe((activity as LifecycleOwner)) { aBoolean: InterstitialAd? ->
                if (aBoolean != null) {
                    interModel.mutable.removeObservers((activity as LifecycleOwner))
                    isClick = false
                    Handler(Looper.getMainLooper()).postDelayed({
                        Log.d("===DelayLoad", "delay")
                        aBoolean.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdDismissedFullScreenContent() {
                                isAdShowing = false
                                if (AdsOnResume.instance!!.isInitialized) {
                                    AdsOnResume.instance!!.isAppResumeEnabled = true
                                }
                                isClick = false
                                //Set inter = null
                                interModel.inter = null
                                interModel.mutable.removeObservers((activity as LifecycleOwner))
                                interModel.mutable.value = null
                                adCallback?.onEventClickAdClosed()
                                dismissAdDialog()
                                Log.d("TAG", "The ad was dismissed.")
                            }

                            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                isAdShowing = false
                                if (AdsOnResume.instance!!.isInitialized) {
                                    AdsOnResume.instance!!.isAppResumeEnabled = true
                                }
                                isClick = false
                                isAdShowing = false
                                //Set inter = null
                                interModel.inter = null
                                dismissAdDialog()
                                Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                interModel.mutable.removeObservers((activity as LifecycleOwner))
                                interModel.mutable.value = null
                                handler.removeCallbacksAndMessages(null)
                                adCallback?.onAdFail(adError.message)
                            }

                            override fun onAdShowedFullScreenContent() {
                                handler.removeCallbacksAndMessages(null)
                                isAdShowing = true
                                adCallback?.onAdShowed()
                                try {
                                    aBoolean.setOnPaidEventListener { adValue ->
                                        adCallback?.onPaid(adValue, interModel.inter?.adUnitId)
                                    }
                                } catch (e: Exception) {
                                }
                            }
                        }
                        showInterstitialAdNew(activity, aBoolean, adCallback)
                    }, 400)
                } else {
                    interModel.check = true
                }
            }
            return
        }
        //Load inter done
        if (interModel.inter == null) {
            if (adCallback != null) {
                isAdShowing = false
                if (AdsOnResume.instance!!.isInitialized) {
                    AdsOnResume.instance!!.isAppResumeEnabled = true
                }
                adCallback.onAdFail("inter null")
                handler.removeCallbacksAndMessages(null)
            }
        } else {
            if (enableLoadingDialog) {
                dialogLoading(activity)
            }
            Handler(Looper.getMainLooper()).postDelayed({
                interModel.inter?.fullScreenContentCallback =
                    object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            isAdShowing = false
                            if (AdsOnResume.instance!!.isInitialized) {
                                AdsOnResume.instance!!.isAppResumeEnabled = true
                            }
                            isClick = false
                            interModel.mutable.removeObservers((activity as LifecycleOwner))
                            interModel.inter = null
                            adCallback?.onEventClickAdClosed()
                            dismissAdDialog()
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            isAdShowing = false
                            if (AdsOnResume.instance!!.isInitialized) {
                                AdsOnResume.instance!!.isAppResumeEnabled = true
                            }
                            handler.removeCallbacksAndMessages(null)
                            isClick = false
                            interModel.inter = null
                            interModel.mutable.removeObservers((activity as LifecycleOwner))
                            isAdShowing = false
                            dismissAdDialog()
                            adCallback?.onAdFail(adError.message)
                            Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                            Log.e("Admodfail", "errorCodeAds" + adError.cause)
                        }

                        override fun onAdShowedFullScreenContent() {
                            handler.removeCallbacksAndMessages(null)
                            isAdShowing = true
                            adCallback?.onAdShowed()
                        }
                    }
                showInterstitialAdNew(activity, interModel.inter, adCallback)
            }, 400)
        }
    }

    @JvmStatic
    private fun showInterstitialAdNew(
        activity: Activity,
        mInterstitialAd: InterstitialAd?,
        callback: CallBackAdsInter?
    ) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
            isAdShowing = true
            Handler(Looper.getMainLooper()).postDelayed({
                callback?.onStartAction()
                mInterstitialAd.setOnPaidEventListener { adValue ->
                    callback?.onPaid(
                        adValue,
                        mInterstitialAd.adUnitId
                    )
                }
                mInterstitialAd.show(activity)
            }, 400)
        } else {
            isAdShowing = false
            if (AdsOnResume.instance!!.isInitialized) {
                AdsOnResume.instance!!.isAppResumeEnabled = true
            }
            dismissAdDialog()
            callback?.onAdFail("onResume")
        }
    }

    @JvmStatic
    fun dismissAdDialog() {
        try {
            if (dialog != null && dialog!!.isShowing) {
                dialog!!.dismiss()
            }
            if (dialogFullScreen != null && dialogFullScreen?.isShowing == true) {
                dialogFullScreen?.dismiss()
            }
        } catch (_: Exception) {

        }
    }

    @JvmStatic
    fun loadAndShowAdRewardWithCallback(
        activity: Activity,
        admobId: String?,
        adCallback2: CallbackRewardAd,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId
        mInterstitialAd = null
        isAdShowing = false
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback2.onAdClosed()
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_reward_id)
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        isAdShowing = false
        if (AdsOnResume.instance!!.isInitialized) {
            AdsOnResume.instance!!.isAppResumeEnabled = false
        }
        RewardedAd.load(activity, admobId!!,
            adRequest!!, object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    // Handle the error.
                    mRewardedAd = null
                    adCallback2.onAdFail(loadAdError.message)
                    dismissAdDialog()
                    if (AdsOnResume.instance!!.isInitialized) {
                        AdsOnResume.instance!!.isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    Log.e("Admodfail", "onAdFailedToLoad" + loadAdError.message)
                    Log.e("Admodfail", "errorCodeAds" + loadAdError.cause)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    mRewardedAd = rewardedAd
                    if (mRewardedAd != null) {
                        mRewardedAd?.setOnPaidEventListener {
                            adCallback2.onPaid(
                                it,
                                mRewardedAd?.adUnitId
                            )
                        }
                        mRewardedAd?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback2.onAdShowed()
                                    if (AdsOnResume.instance!!.isInitialized) {
                                        AdsOnResume.instance!!.isAppResumeEnabled = false
                                    }
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    // Called when ad fails to show.
                                    if (adError.code != 1) {
                                        isAdShowing = false
                                        adCallback2.onAdFail(adError.message)
                                        mRewardedAd = null
                                        dismissAdDialog()
                                    }
                                    if (AdsOnResume.instance!!.isInitialized) {
                                        AdsOnResume.instance!!.isAppResumeEnabled = true
                                    }
                                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                }

                                override fun onAdDismissedFullScreenContent() {
                                    // Called when ad is dismissed.
                                    // Set the ad reference to null so you don't show the ad a second time.
                                    mRewardedAd = null
                                    isAdShowing = false
                                    adCallback2.onAdClosed()
                                    if (AdsOnResume.instance!!.isInitialized) {
                                        AdsOnResume.instance!!.isAppResumeEnabled = true
                                    }
                                }
                            }
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                            if (AdsOnResume.instance!!.isInitialized) {
                                AdsOnResume.instance!!.isAppResumeEnabled = false
                            }
                            mRewardedAd?.show(activity) { adCallback2.onEarned() }
                            isAdShowing = true
                        } else {
                            mRewardedAd = null
                            dismissAdDialog()
                            isAdShowing = false
                            if (AdsOnResume.instance!!.isInitialized) {
                                AdsOnResume.instance!!.isAppResumeEnabled = true
                            }
                        }
                    } else {
                        isAdShowing = false
                        adCallback2.onAdFail("None Show")
                        dismissAdDialog()
                        if (AdsOnResume.instance!!.isInitialized) {
                            AdsOnResume.instance!!.isAppResumeEnabled = true
                        }
                    }
                }
            })
    }

    //Interstitial Reward ads
    @JvmField
    var mInterstitialRewardAd: RewardedInterstitialAd? = null

    @JvmStatic
    fun loadAdInterstitialReward(
        activity: Context,
        mInterstitialRewardAd: RewardedModel,
        callbackAdLoad: CallbackAdLoad
    ) {
        var admobId = mInterstitialRewardAd.ads
        if (!isShowAds || !isNetworkConnected(activity)) {
            return
        }
        if (mInterstitialRewardAd.inter != null) {
            Log.d("===AdsInter", "mInterstitialRewardAd not null")
            return
        }
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        mInterstitialRewardAd.isLoading = true
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_reward_id)
        }
        RewardedInterstitialAd.load(
            activity,
            admobId,
            adRequest!!,
            object : RewardedInterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialRewardAd: RewardedInterstitialAd) {
                    mInterstitialRewardAd.inter = interstitialRewardAd
                    mInterstitialRewardAd.mutable.value = interstitialRewardAd
                    mInterstitialRewardAd.isLoading = false
                    callbackAdLoad.onAdLoaded()
                    Log.i("adLog", "onAdLoaded")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    mInterstitialRewardAd.inter = null
                    mInterstitialRewardAd.isLoading = false
                    mInterstitialRewardAd.mutable.value = null
                    callbackAdLoad.onAdFail(loadAdError.message)
                }
            })
    }

    @JvmStatic
    fun showAdInterstitialRewardWithCallback(
        activity: Activity, mInterstitialRewardAd: RewardedModel,
        adCallback: CallbackRewardAd
    ) {
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            if (AdsOnResume.instance!!.isInitialized) {
                AdsOnResume.instance!!.isAppResumeEnabled = true
            }
            adCallback.onAdFail("No internet or isShowAds = false")
            return
        }

        if (AdsOnResume.instance!!.isInitialized) {
            if (!AdsOnResume.instance!!.isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AdsOnResume.instance!!.isInitialized) {
                    AdsOnResume.instance!!.isAppResumeEnabled = false
                }
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                if (mInterstitialRewardAd.isLoading) {
                    dialogLoading(activity)
                    delay(800)

                    mInterstitialRewardAd.mutable.observe(activity as LifecycleOwner) { reward: RewardedInterstitialAd? ->
                        reward?.let {
                            mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                            it.setOnPaidEventListener { value ->
                                adCallback.onPaid(value, mInterstitialRewardAd.inter?.adUnitId)
                            }
                            mInterstitialRewardAd.inter?.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdDismissedFullScreenContent() {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AdsOnResume.instance!!.isInitialized) {
                                            AdsOnResume.instance!!.isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdClosed()
                                        Log.d("TAG", "The ad was dismissed.")
                                    }

                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        mInterstitialRewardAd.inter = null
                                        mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                        mInterstitialRewardAd.mutable.value = null
                                        if (AdsOnResume.instance!!.isInitialized) {
                                            AdsOnResume.instance!!.isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        dismissAdDialog()
                                        adCallback.onAdFail(adError.message)
                                        Log.d("TAG", "The ad failed to show.")
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        isAdShowing = true
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                        Log.d("TAG", "The ad was shown.")
                                    }
                                }
                            it.show(activity) { adCallback.onEarned() }
                        }
                    }
                } else {
                    if (mInterstitialRewardAd.inter != null) {
                        dialogLoading(activity)
                        delay(800)

                        mInterstitialRewardAd.inter?.setOnPaidEventListener {
                            adCallback.onPaid(it, mInterstitialRewardAd.inter?.adUnitId)
                        }
                        mInterstitialRewardAd.inter?.fullScreenContentCallback =
                            object : FullScreenContentCallback() {
                                override fun onAdDismissedFullScreenContent() {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AdsOnResume.instance!!.isInitialized) {
                                        AdsOnResume.instance!!.isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdClosed()
                                    Log.d("TAG", "The ad was dismissed.")
                                }

                                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                    mInterstitialRewardAd.inter = null
                                    mInterstitialRewardAd.mutable.removeObservers((activity as LifecycleOwner))
                                    mInterstitialRewardAd.mutable.value = null
                                    if (AdsOnResume.instance!!.isInitialized) {
                                        AdsOnResume.instance!!.isAppResumeEnabled = true
                                    }
                                    isAdShowing = false
                                    dismissAdDialog()
                                    adCallback.onAdFail(adError.message)
                                    Log.d("TAG", "The ad failed to show.")
                                }

                                override fun onAdShowedFullScreenContent() {
                                    isAdShowing = true
                                    adCallback.onAdShowed()
                                    Log.d("TAG", "The ad was shown.")
                                }
                            }
                        mInterstitialRewardAd.inter?.show(activity) { adCallback.onEarned() }

                    } else {
                        isAdShowing = false
                        adCallback.onAdFail("None Show")
                        dismissAdDialog()
                        if (AdsOnResume.instance!!.isInitialized) {
                            AdsOnResume.instance!!.isAppResumeEnabled = true
                        }
                        Log.d("TAG", "Ad did not load.")
                    }
                }
            }
        }
    }


    @JvmStatic
    fun loadAndShowAdInterstitial(
        activity: AppCompatActivity,
        admobId: InterModel,
        adCallback: CallBackAdsInter,
        enableLoadingDialog: Boolean
    ) {
        var admobId = admobId.ads
        mInterstitialAd = null
        isAdShowing = false
        if (adRequest == null) {
            initAdRequest(timeOut)
        }
        if (!isShowAds || !isNetworkConnected(activity)) {
            adCallback.onAdFail("No internet")
            return
        }
        if (AdsOnResume.instance!!.isInitialized) {
            if (!AdsOnResume.instance!!.isAppResumeEnabled) {
                return
            } else {
                isAdShowing = false
                if (AdsOnResume.instance!!.isInitialized) {
                    AdsOnResume.instance!!.isAppResumeEnabled = false
                }
            }
        }
        if (enableLoadingDialog) {
            dialogLoading(activity)
        }
        if (isTesting) {
            admobId = activity.getString(R.string.test_ads_admob_inter_id)
        } else {
            checkIdTest(activity, admobId)
        }
        InterstitialAd.load(
            activity,
            admobId,
            adRequest!!,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    adCallback.onAdLoaded()
                    Handler(Looper.getMainLooper()).postDelayed({
                        mInterstitialAd = interstitialAd
                        if (mInterstitialAd != null) {
                            mInterstitialAd!!.onPaidEventListener =
                                OnPaidEventListener { adValue: AdValue? ->
                                    adCallback.onPaid(
                                        adValue,
                                        mInterstitialAd?.adUnitId
                                    )
                                }
                            mInterstitialAd!!.fullScreenContentCallback =
                                object : FullScreenContentCallback() {
                                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                                        adCallback.onAdFail(adError.message)
                                        isAdShowing = false
                                        if (AdsOnResume.instance!!.isInitialized) {
                                            AdsOnResume.instance!!.isAppResumeEnabled = true
                                        }
                                        isAdShowing = false
                                        if (mInterstitialAd != null) {
                                            mInterstitialAd = null
                                        }
                                        dismissAdDialog()
                                        Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                                        Log.e("Admodfail", "errorCodeAds" + adError.cause)
                                    }

                                    override fun onAdDismissedFullScreenContent() {
                                        lastTimeShowInterstitial = Date().time
                                        adCallback.onEventClickAdClosed()
                                        if (mInterstitialAd != null) {
                                            mInterstitialAd = null
                                        }
                                        isAdShowing = false
                                        if (AdsOnResume.instance!!.isInitialized) {
                                            AdsOnResume.instance!!.isAppResumeEnabled = true
                                        }
                                    }

                                    override fun onAdShowedFullScreenContent() {
                                        super.onAdShowedFullScreenContent()
                                        Log.e("===onAdShowed", "onAdShowedFullScreenContent")
                                        adCallback.onAdShowed()
                                        Handler(Looper.getMainLooper()).postDelayed({
                                            dismissAdDialog()
                                        }, 800)
                                    }
                                }
                            if (activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) && mInterstitialAd != null) {
                                adCallback.onStartAction()
                                mInterstitialAd!!.show(activity)
                                isAdShowing = true
                            } else {
                                mInterstitialAd = null
                                dismissAdDialog()
                                isAdShowing = false
                                if (AdsOnResume.instance!!.isInitialized) {
                                    AdsOnResume.instance!!.isAppResumeEnabled = true
                                }
                                adCallback.onAdFail("Interstitial can't show in background")
                            }
                        } else {
                            dismissAdDialog()
                            adCallback.onAdFail("mInterstitialAd null")
                            isAdShowing = false
                            if (AdsOnResume.instance!!.isInitialized) {
                                AdsOnResume.instance!!.isAppResumeEnabled = true
                            }
                        }
                    }, 800)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    mInterstitialAd = null
                    if (AdsOnResume.instance!!.isInitialized) {
                        AdsOnResume.instance!!.isAppResumeEnabled = true
                    }
                    isAdShowing = false
                    adCallback.onAdFail(loadAdError.message)
                    dismissAdDialog()
                }
            })
    }

    //Update New Lib
    private fun checkIdTest(activity: Activity, admobId: String?) {
//        if (admobId.equals(activity.getString(R.string.test_ads_admob_inter_id)) && !BuildConfig.DEBUG) {
//            if (dialog != null) {
//                dialog.dismiss();
//            }
//            Utils.instance!!.showDialogTitle(activity, "Warning", "Build bản release nhưng đang để id test ads", "Đã biết", DialogType.WARNING_TYPE, false, "", new DialogCallback() {
//                @Override
//                public void onClosed() {
//                }
//
//                @Override
//                public void cancel() {
//                }
//            });
//        }
    }

    private val currentTime: Long
        private get() = System.currentTimeMillis()

    fun getDeviceID(context: Context): String {
        val android_id = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ANDROID_ID
        )
        return md5(android_id).uppercase(Locale.getDefault())
    }


    fun md5(s: String): String {
        try {
            // Create MD5 Hash
            val digest = MessageDigest.getInstance("MD5")
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuffer()
            for (i in messageDigest.indices) hexString.append(Integer.toHexString(0xFF and messageDigest[i].toInt()))
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }


    fun dialogLoading(context: Activity) {
        dialogFullScreen = Dialog(context)
        dialogFullScreen?.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen?.setContentView(R.layout.dialog_full_screen)
        dialogFullScreen?.setCancelable(false)
        dialogFullScreen?.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialogFullScreen?.window!!.setLayout(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.MATCH_PARENT
        )
        val img = dialogFullScreen?.findViewById<LottieAnimationView>(R.id.imageView3)
        img?.setAnimation(R.raw.gifloading)
        try {
            if (!context.isFinishing && dialogFullScreen != null && dialogFullScreen?.isShowing == false) {
                dialogFullScreen?.show()
            }
        } catch (ignored: Exception) {
        }

    }


    @JvmStatic
    fun loadAndShowNativeAdsWithLayoutAdsNoBtn(
        activity: Activity,
        nativeModel: NativeModel,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        adCallback: NativeAdCallbackNew
    ) {
        Log.d("===Native", "Native1")
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        viewGroup.removeAllViews()
        var s = nativeModel.ads
        val tagView: View = if (size === SizeNative.UNIFIED_MEDIUM) {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
        } else {
            activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
        }
        viewGroup.addView(tagView, 0)
        val shimmerFrameLayout =
            tagView.findViewById<ShimmerFrameLayout>(R.id.shimmer_view_container)
        shimmerFrameLayout.startShimmer()

        if (isTesting) {
            s = activity.getString(R.string.test_ads_admob_native_id)
        }
        val adLoader = AdLoader.Builder(activity, s)
            .forNativeAd { nativeAd ->
                adCallback.onNativeAdLoaded()
                val adView = activity.layoutInflater
                    .inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeAd, adView, size)
                shimmerFrameLayout.stopShimmer()
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                nativeAd.setOnPaidEventListener { adValue: AdValue ->
                    adCallback.onAdPaid(adValue, s)
                }
                //viewGroup.setVisibility(View.VISIBLE);
            }.withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Log.e("Admodfail", "onAdFailedToLoad" + adError.message)
                    Log.e("Admodfail", "errorCodeAds" + adError.cause)
                    shimmerFrameLayout.stopShimmer()
                    viewGroup.removeAllViews()
                    nativeModel.isLoad = false
                    adCallback.onAdFail(adError.message)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    adCallback.onClickAds()
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build()).build()
        if (adRequest != null) {
            adLoader.loadAd(adRequest!!)
        }
        Log.e("Admod", "loadAdNativeAds")
    }

    @JvmStatic
    fun showNativeAdsWithLayoutNoBtn(
        activity: Activity,
        nativeModel: NativeModel,
        viewGroup: ViewGroup,
        layout: Int,
        size: SizeNative,
        callback: AdsNativeCallBackAdmod
    ) {
        if (!isShowAds || !isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        if (shimmerFrameLayout != null) {
            shimmerFrameLayout?.stopShimmer()
        }
        viewGroup.removeAllViews()
        if (!nativeModel.isLoad) {
            if (nativeModel.nativeAd != null) {
                val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                populateNativeAdViewNoBtn(nativeModel.nativeAd!!, adView, size)
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                viewGroup.removeAllViews()
                viewGroup.addView(adView)
                callback.NativeLoaded()
            } else {
                if (shimmerFrameLayout != null) {
                    shimmerFrameLayout?.stopShimmer()
                }
                nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                callback.NativeFailed("None Show")
            }
        } else {
            val tagView: View = if (size === SizeNative.UNIFIED_MEDIUM) {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_medium, null, false)
            } else {
                activity.layoutInflater.inflate(R.layout.layoutnative_loading_small, null, false)
            }
            viewGroup.addView(tagView, 0)
            if (shimmerFrameLayout == null) shimmerFrameLayout =
                tagView.findViewById(R.id.shimmer_view_container)
            shimmerFrameLayout?.startShimmer()
            nativeModel.native_mutable.observe((activity as LifecycleOwner)) { nativeAd: NativeAd? ->
                if (nativeAd != null) {
                    nativeAd.setOnPaidEventListener {
                        callback.onPaidNative(it, nativeModel.ads)
                    }
                    val adView = activity.layoutInflater.inflate(layout, null) as NativeAdView
                    populateNativeAdViewNoBtn(nativeAd, adView, size)
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    viewGroup.removeAllViews()
                    viewGroup.addView(adView)
                    callback.NativeLoaded()
                    nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                } else {
                    if (shimmerFrameLayout != null) {
                        shimmerFrameLayout?.stopShimmer()
                    }
                    callback.NativeFailed("None Show")
                    nativeModel.native_mutable.removeObservers((activity as LifecycleOwner))
                }
            }
        }
    }
}