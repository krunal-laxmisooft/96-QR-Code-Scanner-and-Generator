package com.app.vv.infscan.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import com.app.vv.infscan.BuildConfig
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.ui.components.AdBannerFrame
import com.app.vv.infscan.ui.theme.InfColors
import com.app.vv.infscan.ui.theme.InfTheme
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize
import com.google.android.libraries.ads.mobile.sdk.banner.AdView
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadCallback
import com.google.android.libraries.ads.mobile.sdk.common.AdValue
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import timber.log.Timber

val LocalAdManager = compositionLocalOf<AdManager?> { null }
val LocalAnalytics = compositionLocalOf<AnalyticsTracker?> { null }

private val mainHandler = Handler(Looper.getMainLooper())

tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/** Standard 320x50 banner. Reserves its space while loading and collapses if no ad is returned. */
@Composable
fun BannerAdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    val manager = LocalAdManager.current ?: return
    val analytics = LocalAnalytics.current
    val canShow by manager.canShowAds.collectAsState()
    if (!canShow) return
    var failed by remember { mutableStateOf(false) }
    var loaded by remember { mutableStateOf(false) }
    if (failed) return
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        if (!loaded) AdBannerFrame()
        AndroidView(
            factory = { ctx ->
                AdView(ctx).also { view ->
                    view.loadAd(
                        BannerAdRequest.Builder(BuildConfig.AD_BANNER, AdSize.BANNER).build(),
                        object : AdLoadCallback<BannerAd> {
                            override fun onAdLoaded(ad: BannerAd) {
                                mainHandler.post { loaded = true }
                                var logged = false
                                ad.adEventCallback = object : BannerAdEventCallback {
                                    override fun onAdPaid(value: AdValue) {
                                        if (logged || analytics == null) return
                                        logged = true
                                        logAdImpression(analytics, AdFormatName.BANNER, placement, value)
                                    }
                                }
                            }

                            override fun onAdFailedToLoad(adError: LoadAdError) {
                                Timber.w("Banner failed: %s", adError.message)
                                mainHandler.post { failed = true }
                            }
                        },
                    )
                }
            },
            onRelease = { it.destroy() },
            modifier = Modifier.wrapContentHeight(),
        )
    }
}

/** Native ad drawn with views so the SDK can track clicks, styled with the design tokens. */
@Composable
fun NativeAdSlot(placement: AdPlacement, modifier: Modifier = Modifier) {
    val manager = LocalAdManager.current ?: return
    val analytics = LocalAnalytics.current
    val canShow by manager.canShowAds.collectAsState()
    if (!canShow) return
    val colors = InfTheme.colors
    var ad by remember { mutableStateOf<NativeAd?>(null) }
    LaunchedEffect(Unit) {
        NativeAdLoader.load(
            NativeAdRequest.Builder(BuildConfig.AD_NATIVE, listOf(NativeAd.NativeAdType.NATIVE)).build(),
            object : NativeAdLoaderCallback {
                override fun onNativeAdLoaded(nativeAd: NativeAd) {
                    var logged = false
                    nativeAd.adEventCallback = object : NativeAdEventCallback {
                        override fun onAdPaid(value: AdValue) {
                            if (logged || analytics == null) return
                            logged = true
                            logAdImpression(analytics, AdFormatName.NATIVE, placement, value)
                        }
                    }
                    mainHandler.post { ad = nativeAd }
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    Timber.w("Native failed: %s", adError.message)
                }
            },
        )
    }
    DisposableEffect(ad) { onDispose { ad?.destroy() } }
    val loadedAd = ad ?: return
    AndroidView(
        factory = { ctx -> buildNativeView(ctx, colors) },
        update = { view -> bindNative(view, loadedAd) },
        onRelease = { it.destroy() },
        modifier = modifier.fillMaxWidth(),
    )
}

private fun Context.dp(value: Int): Int =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()

private fun Context.dpF(value: Int): Float =
    TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics)

private fun text(ctx: Context, sizeSp: Float, color: Int, bold: Boolean = false, lines: Int = 1) = TextView(ctx).apply {
    setTextSize(TypedValue.COMPLEX_UNIT_SP, sizeSp)
    setTextColor(color)
    maxLines = lines
    ellipsize = android.text.TextUtils.TruncateAt.END
    if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
}

private fun roundedBackground(ctx: Context, fill: Int, radiusDp: Int, strokeColor: Int? = null) = GradientDrawable().apply {
    setColor(fill)
    cornerRadius = ctx.dpF(radiusDp)
    if (strokeColor != null) setStroke(ctx.dp(1), strokeColor)
}

private fun buildNativeView(ctx: Context, c: InfColors): NativeAdView {
    val root = NativeAdView(ctx)
    val row = LinearLayout(ctx).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(ctx.dp(12), ctx.dp(12), ctx.dp(12), ctx.dp(12))
        background = roundedBackground(ctx, c.surface.toArgb(), 16, c.outline.toArgb())
    }
    val media = MediaView(ctx).apply {
        layoutParams = LinearLayout.LayoutParams(ctx.dp(72), ctx.dp(72)).apply { marginEnd = ctx.dp(12) }
        background = roundedBackground(ctx, c.surfaceVariant.toArgb(), 12)
        clipToOutline = true
    }
    val column = LinearLayout(ctx).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
    }
    val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
    val badge = text(ctx, 10f, c.onAccent.toArgb(), bold = true).apply {
        this.text = ctx.getString(com.app.vv.infscan.R.string.ad_label)
        setPadding(ctx.dp(6), ctx.dp(2), ctx.dp(6), ctx.dp(2))
        background = roundedBackground(ctx, c.accent.toArgb(), 4)
    }
    val advertiser = text(ctx, 12f, c.inkSecondary.toArgb()).apply { setPadding(ctx.dp(8), 0, 0, 0) }
    top.addView(badge)
    top.addView(advertiser)
    val headline = text(ctx, 15f, c.ink.toArgb(), bold = true, lines = 2)
    val body = text(ctx, 13f, c.inkSecondary.toArgb(), lines = 2)
    val cta = text(ctx, 14f, c.onSoft.toArgb(), bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ctx.dp(36)
        setPadding(ctx.dp(16), 0, ctx.dp(16), 0)
        background = roundedBackground(ctx, c.soft.toArgb(), 18)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = ctx.dp(4) }
    }
    listOf(top, headline, body, cta).forEach(column::addView)
    row.addView(media)
    row.addView(column)
    root.addView(row)
    root.tag = media
    root.headlineView = headline
    root.bodyView = body
    root.callToActionView = cta
    root.advertiserView = advertiser
    return root
}

private fun bindNative(view: NativeAdView, ad: NativeAd) {
    (view.headlineView as TextView).text = ad.headline
    (view.bodyView as TextView).text = ad.body.orEmpty()
    (view.callToActionView as TextView).text = ad.callToAction.orEmpty()
    (view.advertiserView as TextView).text = ad.advertiser.orEmpty()
    val media = view.tag as? MediaView ?: return
    view.registerNativeAd(ad, media)
    view.visibility = View.VISIBLE
    media.mediaContent = ad.mediaContent
}
