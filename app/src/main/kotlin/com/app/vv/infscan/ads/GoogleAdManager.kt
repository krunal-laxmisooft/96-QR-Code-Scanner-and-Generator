package com.app.vv.infscan.ads

import android.app.Activity
import android.content.Context
import com.app.vv.infscan.BuildConfig
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.core.ApplicationScope
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * The only class that talks to the Google Mobile Ads SDK for full-screen formats. Screens use the
 * AdManager interface, and the banner and native slots live next to it in this package.
 */
@Singleton
class GoogleAdManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settings: SettingsRepository,
    private val analytics: AnalyticsTracker,
    @ApplicationScope private val scope: CoroutineScope,
) : AdManager {

    private val _canShowAds = MutableStateFlow(false)
    override val canShowAds: StateFlow<Boolean> = _canShowAds.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    override val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private val startMutex = Mutex()
    private val interstitialPolicy = InterstitialPolicy()
    private var appOpenAttempted = false
    private var appOpenRequest: AppOpenRequest? = null
    private var fullScreenShowing = false

    override suspend fun start(activity: Activity) = startMutex.withLock {
        val consent = UserMessagingPlatform.getConsentInformation(context)
        requestConsent(activity, consent)
        _privacyOptionsRequired.value =
            consent.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (consent.canRequestAds()) initializeSdk()
    }

    private suspend fun requestConsent(activity: Activity, consent: ConsentInformation) {
        suspendCancellableCoroutine { cont ->
            consent.requestConsentInfoUpdate(
                activity,
                ConsentRequestParameters.Builder().build(),
                {
                    UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { error ->
                        if (error != null) Timber.w("Consent form: %s", error.message)
                        if (cont.isActive) cont.resume(Unit)
                    }
                },
                { error ->
                    Timber.w("Consent info: %s", error.message)
                    if (cont.isActive) cont.resume(Unit)
                },
            )
        }
    }

    private suspend fun initializeSdk() {
        if (_canShowAds.value) return
        withContext(Dispatchers.IO) {
            MobileAds.initialize(context, InitializationConfig.Builder(appId()).build())
        }
        preloadAll()
        _canShowAds.value = true
    }

    private fun appId(): String = context.packageManager
        .getApplicationInfo(context.packageName, android.content.pm.PackageManager.GET_META_DATA)
        .metaData.getString("com.google.android.gms.ads.APPLICATION_ID").orEmpty()

    private fun preloadAll() {
        AppOpenAdPreloader.start(
            BuildConfig.AD_APP_OPEN,
            PreloadConfiguration(AppOpenAdRequest.Builder(BuildConfig.AD_APP_OPEN).build()),
            object : PreloadCallback {
                override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                    scope.launch { tryShowAppOpen() }
                }
            },
        )
        InterstitialAdPreloader.start(BuildConfig.AD_INTERSTITIAL, PreloadConfiguration(AdRequest.Builder(BuildConfig.AD_INTERSTITIAL).build()))
        RewardedAdPreloader.start(BuildConfig.AD_REWARDED, PreloadConfiguration(AdRequest.Builder(BuildConfig.AD_REWARDED).build()))
    }

    override fun showAppOpenIfAllowed(activity: Activity, launchedBefore: Boolean) {
        if (appOpenRequest != null || appOpenAttempted) return
        appOpenAttempted = true
        appOpenRequest = AppOpenRequest(WeakReference(activity), launchedBefore, System.currentTimeMillis() + APP_OPEN_WINDOW_MS)
        scope.launch { tryShowAppOpen() }
    }

    /** Shows the cold-start ad only if it is ready within a short window, so it never delays the app. */
    private suspend fun tryShowAppOpen() {
        val request = appOpenRequest ?: return
        val now = System.currentTimeMillis()
        val activity = request.activity.get()
        if (activity == null || now > request.deadline || !_canShowAds.value || fullScreenShowing) {
            if (activity == null || now > request.deadline) appOpenRequest = null
            return
        }
        if (!AppOpenAdPreloader.isAdAvailable(BuildConfig.AD_APP_OPEN)) return
        appOpenRequest = null
        val allowed = AppOpenPolicy.canShow(now, settings.firstLaunchAt(), request.launchedBefore, settings.lastAppOpenAdAt())
        if (!allowed) return
        val ad = AppOpenAdPreloader.pollAd(BuildConfig.AD_APP_OPEN) ?: return
        ad.adEventCallback = object : AppOpenAdEventCallback {
            private val paid = PaidOnce(AdFormatName.APP_OPEN, AdPlacement.APP_OPEN_COLD)
            override fun onAdPaid(value: AdValue) = paid.log(value)
            override fun onAdShowedFullScreenContent() { fullScreenShowing = true }
            override fun onAdDismissedFullScreenContent() { fullScreenShowing = false }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) { fullScreenShowing = false }
        }
        settings.setLastAppOpenAdAt(now)
        ad.show(activity)
    }

    override fun showInterstitial(activity: Activity, placement: AdPlacement, onFinished: () -> Unit) {
        val now = System.currentTimeMillis()
        val ad: InterstitialAd? =
            if (_canShowAds.value && !fullScreenShowing && interstitialPolicy.canShow(now)) {
                InterstitialAdPreloader.pollAd(BuildConfig.AD_INTERSTITIAL)
            } else {
                null
            }
        if (ad == null) {
            onFinished()
            return
        }
        interstitialPolicy.onShown(now)
        ad.adEventCallback = object : InterstitialAdEventCallback {
            private val paid = PaidOnce(AdFormatName.INTERSTITIAL, placement)
            override fun onAdPaid(value: AdValue) = paid.log(value)
            override fun onAdShowedFullScreenContent() { fullScreenShowing = true }
            override fun onAdDismissedFullScreenContent() {
                fullScreenShowing = false
                onFinished()
            }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                fullScreenShowing = false
                onFinished()
            }
        }
        ad.show(activity)
    }

    override fun isRewardedReady(): Boolean = _canShowAds.value && RewardedAdPreloader.isAdAvailable(BuildConfig.AD_REWARDED)

    override fun showRewarded(activity: Activity, onReward: () -> Unit, onFinished: () -> Unit) {
        val ad: RewardedAd? = if (_canShowAds.value && !fullScreenShowing) RewardedAdPreloader.pollAd(BuildConfig.AD_REWARDED) else null
        if (ad == null) {
            onFinished()
            return
        }
        ad.adEventCallback = object : RewardedAdEventCallback {
            private val paid = PaidOnce(AdFormatName.REWARDED, AdPlacement.REWARDED_EXPORT)
            override fun onAdPaid(value: AdValue) = paid.log(value)
            override fun onAdShowedFullScreenContent() { fullScreenShowing = true }
            override fun onAdDismissedFullScreenContent() {
                fullScreenShowing = false
                onFinished()
            }
            override fun onAdFailedToShowFullScreenContent(fullScreenContentError: FullScreenContentError) {
                fullScreenShowing = false
                onFinished()
            }
        }
        ad.show(activity) { onReward() }
    }

    override fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { error ->
            if (error != null) Timber.w("Privacy options: %s", error.message)
            scope.launch {
                val consent = UserMessagingPlatform.getConsentInformation(context)
                if (consent.canRequestAds()) initializeSdk()
            }
        }
    }

    /** Logs one Firebase ad_impression per ad instance, from the SDK paid event. */
    private inner class PaidOnce(private val format: String, private val placement: AdPlacement) {
        private var logged = false

        fun log(value: AdValue) {
            if (logged) return
            logged = true
            logAdImpression(analytics, format, placement, value)
        }
    }
}

private class AppOpenRequest(val activity: WeakReference<Activity>, val launchedBefore: Boolean, val deadline: Long)

private const val APP_OPEN_WINDOW_MS = 5_000L

internal object AdFormatName {
    const val BANNER = "banner"
    const val NATIVE = "native"
    const val INTERSTITIAL = "interstitial"
    const val APP_OPEN = "app_open"
    const val REWARDED = "rewarded"
}

internal fun logAdImpression(analytics: AnalyticsTracker, format: String, placement: AdPlacement, value: AdValue) {
    analytics.log(
        AnalyticsEvents.AD_IMPRESSION,
        mapOf(
            AnalyticsParams.AD_PLATFORM to "Google AdMob",
            AnalyticsParams.AD_SOURCE to "Google",
            AnalyticsParams.AD_FORMAT to format,
            AnalyticsParams.AD_UNIT_NAME to placement.unitName,
            AnalyticsParams.VALUE to value.valueMicros / MICROS,
            AnalyticsParams.CURRENCY to value.currencyCode,
        ),
    )
}

private const val MICROS = 1_000_000.0
