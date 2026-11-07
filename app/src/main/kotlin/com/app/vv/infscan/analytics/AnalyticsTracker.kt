package com.app.vv.infscan.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

interface AnalyticsTracker {
    fun log(event: String, params: Map<String, Any> = emptyMap())
}

object AnalyticsEvents {
    const val ONBOARDING_COMPLETE = "onboarding_complete"
    const val PERMISSION_CAMERA = "permission_camera"
    const val SCAN_SUCCESS = "scan_success"
    const val SCAN_FAIL = "scan_fail"
    const val RESULT_ACTION = "result_action"
    const val CREATE_START = "create_start"
    const val CREATE_COMPLETE = "create_complete"
    const val CREATE_SHARE = "create_share"
    const val PRICE_CHECK = "price_check"
    const val HISTORY_EXPORT = "history_export"
    const val AD_IMPRESSION = "ad_impression"
}

object AnalyticsParams {
    const val TYPE = "type"
    const val SOURCE = "source"
    const val BATCH = "batch"
    const val REASON = "reason"
    const val ACTION = "action"
    const val FOUND = "found"
    const val GRANTED = "granted"
    const val MODE = "mode"
    const val AD_PLATFORM = "ad_platform"
    const val AD_SOURCE = "ad_source"
    const val AD_FORMAT = "ad_format"
    const val AD_UNIT_NAME = "ad_unit_name"
    const val VALUE = "value"
    const val CURRENCY = "currency"
}

/** Sends events to Firebase when the app has a Firebase configuration, otherwise only to the debug log. */
@Singleton
class FirebaseAnalyticsTracker @Inject constructor(
    @ApplicationContext private val context: Context,
) : AnalyticsTracker {

    private val firebase: FirebaseAnalytics? by lazy {
        if (FirebaseApp.getApps(context).isEmpty()) null else FirebaseAnalytics.getInstance(context)
    }

    override fun log(event: String, params: Map<String, Any>) {
        Timber.d("analytics %s %s", event, params)
        val analytics = firebase ?: return
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is String -> bundle.putString(key, value)
                is Int -> bundle.putLong(key, value.toLong())
                is Long -> bundle.putLong(key, value)
                is Double -> bundle.putDouble(key, value)
                is Boolean -> bundle.putString(key, value.toString())
                else -> bundle.putString(key, value.toString())
            }
        }
        analytics.logEvent(event, bundle)
    }
}
