package com.example.libadmob

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.libadmob.ads.AdsAOA
import com.example.libadmob.ads.InitializeAdmob
import com.example.libadmob.ads.AdsOnResume
import com.google.android.gms.ads.AdValue

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        InitializeAdmob.initAdmob(this,10000, true,true)


        ManagerAds.loadInter(this,ManagerAds.INTER)
        ManagerAds.loadNative(this,ManagerAds.NATIVE)

        //ONRESUME
        AdsOnResume.instance!!.init(application, ManagerAds.ONRESUME)

        //AOA
        val appOpenManager = AdsAOA(
            this,
            ManagerAds.AOA,
            20000,
            object : AdsAOA.AppOpenAdsListener {
                override fun onAdPaid(adValue: AdValue, adUnitAds: String) {

                }

                override fun onAdsClose() {

                }

                override fun onAdsFailed(message: String) {

                }

                override fun onAdsLoaded() {

                }
            })
        appOpenManager.loadAoA()

        findViewById<Button>(R.id.btn_inter).setOnClickListener {
            ManagerAds.showInter(this,ManagerAds.INTER, object : ManagerAds.AdListenerNew{
                override fun onCloseClickedOrFailed() {
                    startActivity(Intent(this@MainActivity,InterActivity::class.java))
                }

            },true,true)
        }

        findViewById<Button>(R.id.btn_native).setOnClickListener {
            startActivity(Intent(this,NativeActivity::class.java))
        }

        findViewById<Button>(R.id.btn_banner_collapse).setOnClickListener {
            startActivity(Intent(this,BannerCollapseActivity::class.java))

        }

        findViewById<Button>(R.id.btn_banner).setOnClickListener {
            startActivity(Intent(this,BannerActivity::class.java))

        }
    }
}