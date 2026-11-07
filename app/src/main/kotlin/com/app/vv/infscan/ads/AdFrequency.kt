package com.app.vv.infscan.ads

import java.util.concurrent.TimeUnit

/** Frequency rules from the PRD: interstitial max 1 per 3 minutes and 3 per session. */
class InterstitialPolicy(
    private val minGapMillis: Long = TimeUnit.MINUTES.toMillis(3),
    private val maxPerSession: Int = 3,
) {
    private var shown = 0
    private var lastShownAt = Long.MIN_VALUE

    fun canShow(now: Long): Boolean =
        shown < maxPerSession && (lastShownAt == Long.MIN_VALUE || now - lastShownAt >= minGapMillis)

    fun onShown(now: Long) {
        shown++
        lastShownAt = now
    }
}

/** App open ads skip the first launch and show at most once per 4 hours, on cold start only. */
object AppOpenPolicy {
    private val minGapMillis = TimeUnit.HOURS.toMillis(4)

    fun canShow(now: Long, firstLaunchAt: Long, launchedBefore: Boolean, lastShownAt: Long): Boolean =
        launchedBefore && now > firstLaunchAt && (lastShownAt == 0L || now - lastShownAt >= minGapMillis)
}
