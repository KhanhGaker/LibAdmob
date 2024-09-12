package com.example.libadmob.ads

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.Window
import android.widget.LinearLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.libadmob.R
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import java.util.Date
import kotlin.concurrent.Volatile

class AdsOnResume : ActivityLifecycleCallbacks, LifecycleObserver {
    private var appResumeAd: AppOpenAd? = null
    private val splashAd: AppOpenAd? = null
    private var loadCallback: AppOpenAdLoadCallback? = null
    private var fullScreenContentCallback: FullScreenContentCallback? = null
    private var appResumeAdId: String? = null
    private var currentActivity: Activity? = null
    private var myApplication: Application? = null
    var isShowingAdsOnResume: Boolean = false
    var isShowingAdsOnResumeBanner: Boolean = false
    private var appResumeLoadTime: Long = 0
    private val splashLoadTime: Long = 0
    private val splashTimeout = 0
    var isInitialized: Boolean = false
        private set
    var isAppResumeEnabled: Boolean = true
    private val disabledAppOpenList: MutableList<Class<*>> = ArrayList()
    private val splashActivity: Class<*>? = null
    private var isTimeout = false
    private var dialogFullScreen: Dialog? = null
    private var timeToBackground: Long = 0
    private var waitingTime: Long = 0
    private val timeoutHandler = Handler { msg: Message ->
        if (msg.what == TIMEOUT_MSG) {
            isTimeout = true
        }
        false
    }

    fun setWaitingTime(waitingTime: Long) {
        this.waitingTime = waitingTime
    }

    fun setTimeToBackground(timeToBackground: Long) {
        this.timeToBackground = timeToBackground
    }

    /**
     * Init AppOpenManager
     *
     * @param application
     */
    fun init(application: Application, appOpenAdId: String?) {
        isInitialized = true
        this.myApplication = application
        initAdRequest()
        if (InitializeAdmob.isTesting) {
            this.appResumeAdId = application.getString(R.string.test_ads_admob_app_open_new)
        } else {
            this.appResumeAdId = appOpenAdId
        }
        myApplication!!.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        if (!isAdAvailable(false) && appOpenAdId != null) {
            fetchAd(false)
        }
    }

    var adRequest: AdRequest? = null

    // get AdRequest
    fun initAdRequest() {
        adRequest = AdRequest.Builder()
            .setHttpTimeoutMillis(5000)
            .build()
    }

    val isShowingAd: Boolean
        /**
         * Check app open ads is showing
         *
         * @return
         */
        get() = Companion.isShowingAd

    /**
     * Disable app open app on specific activity
     *
     * @param activityClass
     */
    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "disableAppResumeWithActivity: " + activityClass.name)
        disabledAppOpenList.add(activityClass)
    }

    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        Log.d(TAG, "enableAppResumeWithActivity: " + activityClass.name)
        Handler().postDelayed({
            disabledAppOpenList.remove(
                activityClass
            )
        }, 40)
    }


    fun setAppResumeAdId(appResumeAdId: String?) {
        this.appResumeAdId = appResumeAdId
    }

    fun setFullScreenContentCallback(callback: FullScreenContentCallback?) {
        this.fullScreenContentCallback = callback
    }

    fun removeFullScreenContentCallback() {
        this.fullScreenContentCallback = null
    }

    var isLoading: Boolean = false
    var isDismiss: Boolean = false
    fun fetchAd(isSplash: Boolean) {
        Log.d(
            TAG,
            "fetchAd: isSplash = $isSplash"
        )
        if (isAdAvailable(isSplash) || appResumeAdId == null || this@AdsOnResume.appResumeAd != null) {
            Log.d(TAG, "AppOpenManager: Ad is ready or id = null")
            return
        }
        if (!isLoading) {
            Log.d(TAG, "===fetchAd: Loading")
            isLoading = true
            loadCallback =
                object : AppOpenAdLoadCallback() {
                    /**
                     * Called when an app open ad has loaded.
                     *
                     * @param ad the loaded app open ad.
                     */
                    override fun onAdLoaded(ad: AppOpenAd) {
                        Log.d(TAG, "AppOpenManager: Loaded")
                        this@AdsOnResume.appResumeAd = ad
                        this@AdsOnResume.appResumeLoadTime =
                            Date().time
                    }

                    /**
                     * Called when an app open ad has failed to load.
                     *
                     * @param loadAdError the error.
                     */
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        // Handle the error.
                        isLoading = false
                        Log.d(TAG, "AppOpenManager: onAdFailedToLoad")
                        val a = "fail"
                    }
                }
            AppOpenAd.load(
                myApplication!!, appResumeAdId!!, adRequest!!, loadCallback!!
            )
        }
    }


    private fun wasLoadTimeLessThanNHoursAgo(loadTime: Long, numHours: Long): Boolean {
        val dateDifference = Date().time - loadTime
        val numMilliSecondsPerHour: Long = 3600000
        return (dateDifference < (numMilliSecondsPerHour * numHours))
    }


    fun isAdAvailable(isSplash: Boolean): Boolean {
        val loadTime = if (isSplash) splashLoadTime else appResumeLoadTime
        val wasLoadTimeLessThanNHoursAgo = wasLoadTimeLessThanNHoursAgo(loadTime, 4)
        Log.d(
            TAG,
            "isAdAvailable: $wasLoadTimeLessThanNHoursAgo"
        )
        return ((if (isSplash) splashAd != null else appResumeAd != null)
                && wasLoadTimeLessThanNHoursAgo)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d("===ADS", activity.javaClass.toString() + "|" + AdActivity::class.java)
        //        if (activity.getClass() == AdActivity.class){
//            Log.d("===ADS", "Back");
//            return;
//        }
        currentActivity = activity
        Log.d("===ADS", "Running")
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
        if (splashActivity == null) {
            if (activity.javaClass.name != AdActivity::class.java.name) {
                fetchAd(false)
            }
        } else {
            if (activity.javaClass.name != splashActivity.name && activity.javaClass.name != AdActivity::class.java.name) {
                fetchAd(false)
            }
        }
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
//        if (activity.getClass() == AdActivity.class){
//            return;
//        }
        currentActivity = null
        if (dialogFullScreen != null && dialogFullScreen!!.isShowing) {
            dialogFullScreen!!.dismiss()
        }
    }

    fun showAdIfAvailable(isSplash: Boolean) {
        if (!ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Log.d("===Onresume", "STARTED")
            if (fullScreenContentCallback != null) {
                try {
                    dialogFullScreen!!.dismiss()
                    dialogFullScreen = null
                } catch (ignored: Exception) {
                }
                fullScreenContentCallback!!.onAdDismissedFullScreenContent()
            }
            return
        }
        Log.d("===Onresume", "FullScreenContentCallback")
        if (!Companion.isShowingAd && isAdAvailable(isSplash)) {
            isDismiss = true
            val callback: FullScreenContentCallback =
                object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d("==TestAOA==", "onResume: true")
                        Handler().postDelayed({
                            isDismiss = false
                            Log.d("==TestAOA==", "onResume: false")
                        }, 200)
                        isLoading = false
                        Log.d(TAG, "onAdShowedFullScreenContent: Dismiss")
                        try {
                            dialogFullScreen!!.dismiss()
                            dialogFullScreen = null
                        } catch (ignored: Exception) {
                        }
                        // Set the reference to null so isAdAvailable() returns false.
                        appResumeAd = null
                        if (fullScreenContentCallback != null) {
                            fullScreenContentCallback!!.onAdDismissedFullScreenContent()
                        }
                        Companion.isShowingAd = false
                        fetchAd(isSplash)
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        isLoading = false
                        isDismiss = false
                        Log.d(TAG, "onAdShowedFullScreenContent: Show false")
                        try {
                            dialogFullScreen!!.dismiss()
                            dialogFullScreen = null
                        } catch (ignored: Exception) {
                        }

                        if (fullScreenContentCallback != null) {
                            fullScreenContentCallback!!.onAdFailedToShowFullScreenContent(adError!!)
                        }
                        fetchAd(isSplash)
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "onAdShowedFullScreenContent: Show")
                        Companion.isShowingAd = true
                        appResumeAd = null
                    }
                }
            showAdsResume(isSplash, callback)
        } else {
            Log.d(TAG, "Ad is not ready")
            if (!isSplash) {
                fetchAd(false)
            }
        }
    }

    private fun showAdsResume(isSplash: Boolean, callback: FullScreenContentCallback) {
        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            Handler().postDelayed({
                if (appResumeAd != null) {
                    appResumeAd!!.fullScreenContentCallback = callback
                    if (currentActivity != null) {
                        showDialog(currentActivity)
                        appResumeAd!!.show(currentActivity!!)
                    }
                }
            }, 100)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    protected fun onMoveToForeground() {
        // Show the ad (if available) when the app moves to foreground.
        Handler().postDelayed(Runnable {
            Log.d("===Onresume", "onresume")
            if (System.currentTimeMillis() - timeToBackground < waitingTime) {
                return@Runnable
            }
            if (currentActivity == null) {
                return@Runnable
            }
            if (currentActivity!!.javaClass == AdActivity::class.java) {
                return@Runnable
            }
            if (InitializeAdmob.isAdShowing) {
                return@Runnable
            }
            if (!InitializeAdmob.isShowAds) {
                return@Runnable
            }

            if (!isAppResumeEnabled) {
                Log.d("===Onresume", "isAppResumeEnabled")
                return@Runnable
            } else {
                if (InitializeAdmob.dialog != null && InitializeAdmob.dialog!!.isShowing) InitializeAdmob.dialog!!.dismiss()
            }

            for (activity in disabledAppOpenList) {
                if (activity.name == currentActivity!!.javaClass.name) {
                    Log.d(TAG, "onStart: activity is disabled")
                    return@Runnable
                }
            }
            showAdIfAvailable(false)
        }, 30)
    }

    fun showDialog(context: Context?) {
        isShowingAdsOnResume = true
        isShowingAdsOnResumeBanner = true
        dialogFullScreen = Dialog(context!!)
        dialogFullScreen!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialogFullScreen!!.setContentView(R.layout.dialog_full_screen_onresume)
        dialogFullScreen!!.setCancelable(false)
        dialogFullScreen!!.window!!.setBackgroundDrawable(ColorDrawable(Color.WHITE))
        dialogFullScreen!!.window!!
            .setLayout(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
        try {
            if (!currentActivity!!.isFinishing && dialogFullScreen != null && !dialogFullScreen!!.isShowing) {
                dialogFullScreen!!.show()
            }
        } catch (ignored: Exception) {
        }
    }

    companion object {
        private const val TAG = "AppOpenManager"

        @Volatile
        private var INSTANCE: AdsOnResume? = null
        private var isShowingAd = false
        private const val TIMEOUT_MSG = 11

        @get:Synchronized
        val instance: AdsOnResume?
            get() {
                if (INSTANCE == null) {
                    INSTANCE = AdsOnResume()
                }
                return INSTANCE
            }
    }
}