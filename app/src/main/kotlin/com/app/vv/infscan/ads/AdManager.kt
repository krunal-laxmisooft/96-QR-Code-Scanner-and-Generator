package com.app.vv.infscan.ads

import android.app.Activity
import kotlinx.coroutines.flow.StateFlow

interface AdManager {
    /** True once consent allows ad requests and the SDK is initialized. */
    val canShowAds: StateFlow<Boolean>
    val privacyOptionsRequired: StateFlow<Boolean>

    /** Runs the consent flow, then initializes the SDK and preloads ads. Safe to call again. */
    suspend fun start(activity: Activity)
    fun showAppOpenIfAllowed(activity: Activity, launchedBefore: Boolean)
    fun showInterstitial(activity: Activity, placement: AdPlacement, onFinished: () -> Unit)
    fun isRewardedReady(): Boolean
    fun showRewarded(activity: Activity, onReward: () -> Unit, onFinished: () -> Unit)
    fun showPrivacyOptions(activity: Activity)
}
