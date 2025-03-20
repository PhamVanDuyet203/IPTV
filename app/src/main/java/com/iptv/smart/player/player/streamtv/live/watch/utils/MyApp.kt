package com.iptv.smart.player.player.streamtv.live.watch.utils

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.adjust.sdk.Adjust
import com.adjust.sdk.AdjustConfig
import com.adjust.sdk.LogLevel
import com.iptv.smart.player.player.streamtv.live.watch.R
import java.util.Locale

class MyApp : Application() , Application.ActivityLifecycleCallbacks{
    override fun onCreate() {
        super.onCreate()
        registerActivityLifecycleCallbacks(this)
        initAdjust()
    }


    private fun initAdjust() {
        val config = AdjustConfig(
            this,
            getString(R.string.adjust_token_key),
            AdjustConfig.ENVIRONMENT_PRODUCTION
        )
        config.setLogLevel(LogLevel.WARN)
        Adjust.initSdk(config)
        registerActivityLifecycleCallbacks(AdjustLifecycleCallbacks())
    }
    private class AdjustLifecycleCallbacks : ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, bundle: Bundle?) {
        }
        override fun onActivityStarted(activity: Activity) {}
        override fun onActivityResumed(activity: Activity) {
            Adjust.onResume()
        }
        override fun onActivityPaused(activity: Activity) {
            Adjust.onPause()
        }
        override fun onActivityStopped(activity: Activity) {}
        override fun onActivitySaveInstanceState(activity: Activity, bundle: Bundle) {}
        override fun onActivityDestroyed(activity: Activity) {}

    }


    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }

}