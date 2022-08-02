package pl.grzybdev.openmic.client

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd

class GoogleHelper
{
    companion object {
        fun initializeAds(ctx: Context) {
            MobileAds.initialize(ctx)
        }

        fun showStartupAd(activity: Activity, ctx: Context) {
            val adRequest = AdRequest.Builder().build()
            val loadCallback = object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    super.onAdLoaded(ad)
                    ad.show(activity)
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    Log.d(this.javaClass.name, "onAppOpenAdFailedToLoad: ")
                }
            }

            val orientation = activity.resources.configuration.orientation

            AppOpenAd.load(
                ctx,
                activity.getString(R.string.AD_UNIT_ID_BOOT),
                adRequest,
                when (orientation) {
                    Configuration.ORIENTATION_LANDSCAPE -> AppOpenAd.APP_OPEN_AD_ORIENTATION_LANDSCAPE
                    else -> AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT
                },
                loadCallback
            )
        }
    }
}