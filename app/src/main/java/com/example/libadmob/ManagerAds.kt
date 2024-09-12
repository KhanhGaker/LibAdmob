package com.example.libadmob

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.example.libadmob.ads.InitializeAdmob
import com.example.libadmob.ads.AdsOnResume
import com.example.libadmob.data.enum_model.EnumBannerCollapsible
import com.example.libadmob.data.enum_model.SizeNative
import com.example.libadmob.data.model.BannerModel
import com.example.libadmob.data.model.InterModel
import com.example.libadmob.data.model.NativeModel
import com.example.libadmob.listener.CallBackAdsInter
import com.example.libadmob.listener.CallBackLoadInter
import com.example.libadmob.listener.CallbackNativeAd
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdValue
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.nativead.NativeAd


object ManagerAds {

    var ONRESUME = ""
    var AOA = ""
    var INTER = InterModel("")

    var NATIVE = NativeModel("")

    var BANNER_COLLAPSE = BannerModel("")
    var BANNER = ""



    fun loadAndShowAdsInter(
        context: AppCompatActivity,
        interHolder: InterModel,
        callback: AdListenerNew
    ) {
        AdsOnResume.instance!!.isAppResumeEnabled = true
        InitializeAdmob.loadAndShowAdInterstitial(context, interHolder, object : CallBackAdsInter {
            override fun onStartAction() {

            }

            override fun onEventClickAdClosed() {
                callback.onCloseClickedOrFailed()
            }

            override fun onAdShowed() {
                AdsOnResume.instance!!.isAppResumeEnabled = true
                Handler().postDelayed({
                    try {
                        InitializeAdmob.dismissAdDialog()
                    } catch (_: Exception) {

                    }
                }, 800)
            }

            override fun onAdLoaded() {

            }

            override fun onAdFail(p0: String?) {
                callback.onCloseClickedOrFailed()
            }

            override fun onPaid(p0: AdValue?, p1: String?) {

            }

        }, true)
    }

    interface AdListenerNew {
        fun onCloseClickedOrFailed()
    }



    @JvmStatic
    fun loadNative(activity: Context, nativeHolder: NativeModel) {
        InitializeAdmob.loadAndGetNativeAds(activity, nativeHolder, object : CallbackNativeAd {
            override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

            }

            override fun onNativeAdLoaded() {

            }

            override fun onAdFail(error: String?) {

            }

            override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {
            }

        })
    }

    fun showAdBanner(activity: Activity, adsEnum: String, viewGroup: ViewGroup, line: View) {
        InitializeAdmob.loadAdBanner(
            activity,
            adsEnum,
            viewGroup,
            object : InitializeAdmob.BannerCallBack {
                override fun onLoad() {
                    viewGroup.visible()
                    line.visible()
                }

                override fun onClickAds() {

                }

                override fun onFailed(message: String) {
                    viewGroup.gone()
                    line.gone()
                }

                override fun onPaid(adValue: AdValue?, mAdView: AdView?) {

                }

            })
    }

    fun View.gone() {
        visibility = View.GONE
    }

    fun View.visible() {
        visibility = View.VISIBLE
    }

    fun loadInter(context: Context, interHolder: InterModel) {
        InitializeAdmob.loadAndGetAdInterstitial(context, interHolder,
            object : CallBackLoadInter {
                override fun onAdClosed() {

                }

                override fun onEventClickAdClosed() {
                }

                override fun onAdShowed() {
                }

                override fun onAdLoaded(
                    interstitialAd: com.google.android.gms.ads.interstitial.InterstitialAd?,
                    isLoading: Boolean
                ) {

                }

                override fun onAdFail(message: String?) {
                }
            }
        )
    }

    //Hàm Show Interstitial
    fun showInter(
        context: Context,
        interHolder: InterModel,
        adListener: AdListenerNew,
        enableLoadingDialog: Boolean,
        reload: Boolean

    ) {
        AdsOnResume.instance!!.isAppResumeEnabled = true
        InitializeAdmob.showAdInterstitialWithCallbackNotLoadNew(
            context as Activity, interHolder, 8000,
            object : CallBackAdsInter {
                override fun onAdLoaded() {
                }

                override fun onAdFail(error: String?) {
                    adListener.onCloseClickedOrFailed()
                    if (reload) {
                        loadInter(context, interHolder)
                    }
                }

                override fun onPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onStartAction() {
                }

                override fun onEventClickAdClosed() {
                    adListener.onCloseClickedOrFailed()
                    if (reload) {
                        loadInter(context, interHolder)
                    }
                }

                override fun onAdShowed() {
                    AdsOnResume.instance!!.isAppResumeEnabled = false
                    try {
                        InitializeAdmob.dismissAdDialog()
                    } catch (_: Exception) {

                    }
                }
            }, enableLoadingDialog
        )
    }

    @JvmStatic
    fun showAdBannerCollapsible(
        activity: Activity,
        adsEnum: BannerModel,
        view: ViewGroup,
        line: View
    ) {
        if (InitializeAdmob.isNetworkConnected(activity)) {
            InitializeAdmob.loadAdBannerCollapsibleReload(
                activity,
                adsEnum,
                EnumBannerCollapsible.BOTTOM,
                view,
                object : InitializeAdmob.BannerCollapsibleAdCallback {
                    override fun onBannerAdLoaded(adSize: AdSize) {
                        val params: ViewGroup.LayoutParams = view.layoutParams
                        params.height = adSize.getHeightInPixels(activity)
                        view.layoutParams = params
                    }

                    override fun onClickAds() {

                    }

                    override fun onAdFail(message: String) {
                        view.visibility = View.GONE
                        line.visibility = View.GONE
                    }

                    override fun onAdPaid(adValue: AdValue, mAdView: AdView) {

                    }
                })
        } else {
            view.visibility = View.GONE
            line.visibility = View.GONE
        }
    }


    fun loadAndShowAdsNative(activity: Activity, viewGroup: ViewGroup, holder: NativeModel) {
        if (!InitializeAdmob.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        InitializeAdmob.loadAndShowNativeAdsWithLayoutAds(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            SizeNative.UNIFIED_MEDIUM,
            object : InitializeAdmob.NativeAdCallbackNew {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    viewGroup.visible()
                }

                override fun onAdFail(error: String) {
                    viewGroup.gone()
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onClickAds() {

                }


            })
    }

    fun showNativeBtnBottom(activity: Activity, viewGroup: ViewGroup, holder: NativeModel) {
        if (!InitializeAdmob.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        InitializeAdmob.showNativeAdsWithLayout(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            SizeNative.UNIFIED_MEDIUM,
            object : InitializeAdmob.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaidNative(adValue: AdValue, adUnitAds: String) {

                }

                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun loadAndShowNativeNoShimmer(
        activity: Activity,
        viewGroup: ViewGroup,
        holder: NativeModel
    ) {
        if (!InitializeAdmob.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        InitializeAdmob.loadAndShowNativeAdsWithLayoutAdsNoShimmer(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            SizeNative.UNIFIED_MEDIUM,
            object : InitializeAdmob.NativeAdCallbackNew {
                override fun onAdFail(error: String) {
                    viewGroup.visibility = View.GONE
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }

                override fun onClickAds() {

                }

                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                }
            })
    }

//    fun showNativeCustom(activity: Activity, viewGroup: ViewGroup, holder: NativeModel) {
//        if (!InitializeAdmob.isNetworkConnected(activity)) {
//            viewGroup.visibility = View.GONE
//            return
//        }
//        InitializeAdmob.showNativeAdsWithLayout(
//            activity,
//            holder,
//            viewGroup,
//            R.layout.ad_template_small,
//            SizeNative.UNIFIED_SMALL,
//            object : InitializeAdmob.AdsNativeCallBackAdmod {
//                override fun NativeLoaded() {
//                    viewGroup.visible()
//                }
//
//                override fun onPaidNative(adValue: AdValue, adUnitAds: String) {
//
//                }
//
//                override fun NativeFailed(massage: String) {
//                    loadAdsNative(activity, holder)
//                    viewGroup.gone()
//                }
//            })
//    }

//    fun showNativeCustomItemUnit(activity: Activity, viewGroup: ViewGroup, holder: NativeModel,listener: AdListenerNew) {
//        if (!InitializeAdmob.isNetworkConnected(activity)) {
//            viewGroup.visibility = View.GONE
//            listener.onCloseClickedOrFailed()
//            return
//        }
//        InitializeAdmob.showNativeAdsWithLayout(
//            activity,
//            holder,
//            viewGroup,
//            R.layout.ad_template_small,
//            SizeNative.UNIFIED_SMALL,
//            object : InitializeAdmob.AdsNativeCallBackAdmod {
//                override fun NativeLoaded() {
//                    viewGroup.visible()
//                }
//
//                override fun onPaidNative(adValue: AdValue, adUnitAds: String) {
//
//                }
//
//                override fun NativeFailed(massage: String) {
//                    loadAdsNative(activity, holder)
//                    viewGroup.gone()
//                    listener.onCloseClickedOrFailed()
//                }
//            })
//    }

    fun showNativeMedium(activity: Activity, viewGroup: ViewGroup, holder: NativeModel) {
        if (!InitializeAdmob.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        InitializeAdmob.showNativeAdsWithLayout(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            SizeNative.UNIFIED_MEDIUM,
            object : InitializeAdmob.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    viewGroup.visible()
                }

                override fun onPaidNative(adValue: AdValue, adUnitAds: String) {

                }

                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun showNativeMediumIntro(activity: Activity, viewGroup: ViewGroup, holder: NativeModel, show: Boolean) {
        if (!InitializeAdmob.isNetworkConnected(activity)) {
            viewGroup.visibility = View.GONE
            return
        }
        InitializeAdmob.showNativeAdsWithLayout(
            activity,
            holder,
            viewGroup,
            R.layout.ad_template_medium,
            SizeNative.UNIFIED_MEDIUM,
            object : InitializeAdmob.AdsNativeCallBackAdmod {
                override fun NativeLoaded() {
                    if (show){
                        viewGroup.visible()
                    }else {
                        viewGroup.gone()
                    }
                }

                override fun onPaidNative(adValue: AdValue, adUnitAds: String) {

                }

                override fun NativeFailed(massage: String) {
                    loadAdsNative(activity, holder)
                    viewGroup.gone()
                }
            })
    }

    fun loadAdsNative(context: Context, holder: NativeModel) {
        InitializeAdmob.loadAndGetNativeAds(
            context,
            holder,
            object : CallbackNativeAd {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                }

                override fun onAdFail(error: String?) {
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }


            })
    }

    var loadedItemUnit = true
    fun loadAdsNativeUnit(context: Context, holder: NativeModel) {
        InitializeAdmob.loadAndGetNativeAds(
            context,
            holder,
            object : CallbackNativeAd {
                override fun onLoadedAndGetNativeAd(ad: NativeAd?) {

                }

                override fun onNativeAdLoaded() {
                    loadedItemUnit = true
                }

                override fun onAdFail(error: String?) {
                    loadedItemUnit = false
                }

                override fun onAdPaid(adValue: AdValue?, adUnitAds: String?) {

                }


            })
    }
}
