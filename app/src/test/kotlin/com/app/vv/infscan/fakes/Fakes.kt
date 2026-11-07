package com.app.vv.infscan.fakes

import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.data.repo.LinkResolver
import com.app.vv.infscan.data.repo.PriceOffer
import com.app.vv.infscan.data.repo.PriceRepository
import com.app.vv.infscan.data.repo.ProductInfo
import com.app.vv.infscan.data.repo.ProductRepository
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.DetectedCode
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.NewHistoryItem
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.platform.BarcodeImageDecoder
import com.app.vv.infscan.platform.Haptics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class FakeHistoryRepository : HistoryRepository {
    val items = MutableStateFlow<List<HistoryItem>>(emptyList())
    private var nextId = 1L

    override fun observe(kind: HistoryKind): Flow<List<HistoryItem>> =
        items.map { list -> list.filter { it.kind == kind }.sortedWith(compareByDescending<HistoryItem> { it.createdAt }.thenByDescending { it.id }) }

    override fun observeCount(kind: HistoryKind): Flow<Int> = items.map { list -> list.count { it.kind == kind } }
    override fun observeItem(id: Long): Flow<HistoryItem?> = items.map { list -> list.firstOrNull { it.id == id } }
    override suspend fun get(id: Long): HistoryItem? = items.value.firstOrNull { it.id == id }
    override suspend fun latest(kind: HistoryKind, limit: Int): List<HistoryItem> = items.value.filter { it.kind == kind }.sortedByDescending { it.createdAt }.take(limit)
    override suspend fun all(): List<HistoryItem> = items.value.sortedWith(compareByDescending<HistoryItem> { it.createdAt }.thenByDescending { it.id })

    override suspend fun add(item: NewHistoryItem): Long {
        val id = nextId++
        items.value = items.value + HistoryItem(id, item.kind, item.codeKind, item.raw, item.title, item.format, item.source, item.createdAt, false)
        return id
    }

    override suspend fun setFavorite(id: Long, favorite: Boolean) {
        items.value = items.value.map { if (it.id == id) it.copy(favorite = favorite) else it }
    }

    override suspend fun delete(id: Long) {
        items.value = items.value.filterNot { it.id == id }
    }

    override suspend fun restore(item: HistoryItem) {
        items.value = items.value + item
    }

    override suspend fun clear() {
        items.value = emptyList()
    }

    override suspend fun purgeOlderThan(retentionDays: Int, now: Long): Int = 0

    fun seed(
        kind: HistoryKind = HistoryKind.SCANNED,
        codeKind: CodeKind = CodeKind.URL,
        raw: String = "https://example.com",
        title: String = "example.com",
        createdAt: Long = 0L,
        favorite: Boolean = false,
        format: CodeFormat = CodeFormat.QR,
    ): HistoryItem {
        val item = HistoryItem(nextId++, kind, codeKind, raw, title, format, ScanSource.CAMERA, createdAt, favorite)
        items.value = items.value + item
        return item
    }
}

class FakeSettingsRepository(initial: AppSettings = AppSettings()) : SettingsRepository {
    private val state = MutableStateFlow(initial)
    var lastAppOpen = 0L
    override val settings: Flow<AppSettings> = state
    override suspend fun current(): AppSettings = state.value
    override suspend fun setThemeMode(mode: ThemeMode) { state.value = state.value.copy(themeMode = mode) }
    override suspend fun setVibrateOnScan(value: Boolean) { state.value = state.value.copy(vibrateOnScan = value) }
    override suspend fun setOpenLinksWithoutAsking(value: Boolean) { state.value = state.value.copy(openLinksWithoutAsking = value) }
    override suspend fun setRetentionDays(days: Int) { state.value = state.value.copy(retentionDays = days) }
    override suspend fun setOnboardingDone() { state.value = state.value.copy(onboardingDone = true) }
    override suspend fun firstLaunchAt(): Long = 1L
    override suspend fun lastAppOpenAdAt(): Long = lastAppOpen
    override suspend fun setLastAppOpenAdAt(millis: Long) { lastAppOpen = millis }
}

class RecordingAnalytics : AnalyticsTracker {
    val events = mutableListOf<Pair<String, Map<String, Any>>>()
    override fun log(event: String, params: Map<String, Any>) { events += event to params }
    fun names() = events.map { it.first }
}

class FakeHaptics : Haptics {
    var ticks = 0
    override fun scanTick() { ticks++ }
}

class FakeBarcodeDecoder(var result: List<DetectedCode> = emptyList()) : BarcodeImageDecoder {
    override suspend fun decode(uri: String): List<DetectedCode> = result
}

class FakeLinkResolver(var destination: String? = null) : LinkResolver {
    var calls = 0
    override suspend fun resolve(url: String): String? { calls++; return destination }
}

class FakeProductRepository(var result: AppResult<ProductInfo> = AppResult.Failure(FailureReason.NOT_FOUND)) : ProductRepository {
    override suspend fun lookup(code: String): AppResult<ProductInfo> = result
}

class FakePrices(override var isEnabled: Boolean = true, var result: AppResult<List<PriceOffer>> = AppResult.Success(emptyList())) : PriceRepository {
    var calls = 0
    override suspend fun offers(code: String): AppResult<List<PriceOffer>> { calls++; return result }
}

class MutableClock(var millis: Long = 1_000_000L) : Clock() {
    override fun getZone() = ZoneOffset.UTC
    override fun withZone(zone: java.time.ZoneId?): Clock = this
    override fun instant(): Instant = Instant.ofEpochMilli(millis)
}

class FakeAdManager : com.app.vv.infscan.ads.AdManager {
    override val canShowAds = MutableStateFlow(false)
    override val privacyOptionsRequired = MutableStateFlow(false)
    override suspend fun start(activity: android.app.Activity) = Unit
    override fun showAppOpenIfAllowed(activity: android.app.Activity, launchedBefore: Boolean) = Unit
    override fun showInterstitial(activity: android.app.Activity, placement: com.app.vv.infscan.ads.AdPlacement, onFinished: () -> Unit) = onFinished()
    override fun isRewardedReady(): Boolean = false
    override fun showRewarded(activity: android.app.Activity, onReward: () -> Unit, onFinished: () -> Unit) = onFinished()
    override fun showPrivacyOptions(activity: android.app.Activity) = Unit
}

class FakeImageSaver(var succeeds: Boolean = true) : com.app.vv.infscan.platform.ImageSaver {
    val saved = mutableListOf<String>()
    override suspend fun savePng(bitmap: android.graphics.Bitmap, displayName: String): Boolean {
        if (succeeds) saved += displayName
        return succeeds
    }
}

class FakeQrImages : com.app.vv.infscan.platform.QrImageFactory {
    override fun create(payload: String, sizePx: Int): android.graphics.Bitmap =
        android.graphics.Bitmap.createBitmap(1, 1, android.graphics.Bitmap.Config.ARGB_8888)
}
