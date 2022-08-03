package pl.grzybdev.openmic.client

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import pl.grzybdev.openmic.client.activities.MainActivity

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

            val mainActivity = activity as MainActivity
            val lastShown = mainActivity.sharedPrefs.getLong(activity.getString(R.string.PREFERENCE_APP_BOOT_AD_LAST_SHOWN), -1);
            val timeNow = System.currentTimeMillis()

            if (lastShown == -1L || timeNow - lastShown > 60 * 60 * 1000) {
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

                mainActivity.sharedPrefs.edit().putLong(activity.getString(R.string.PREFERENCE_APP_BOOT_AD_LAST_SHOWN), timeNow).apply()
            }
        }
        }
    }
}