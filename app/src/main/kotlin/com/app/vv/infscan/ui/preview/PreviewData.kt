package com.app.vv.infscan.ui.preview

import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.repo.PriceOffer
import com.app.vv.infscan.data.repo.ProductInfo
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.domain.parser.LinkSafety
import com.app.vv.infscan.ui.history.HistoryUiState
import com.app.vv.infscan.ui.result.DetailsState
import com.app.vv.infscan.ui.result.LinkUiState
import com.app.vv.infscan.ui.result.PricesState
import com.app.vv.infscan.ui.result.ProductTab
import com.app.vv.infscan.ui.result.ProductUiState
import com.app.vv.infscan.ui.result.ResultUiState
import com.app.vv.infscan.ui.scan.BatchEntry
import com.app.vv.infscan.ui.settings.SettingsUiState
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Sample state that matches the design canvas copy, used by previews and by the screenshot tests. */
object PreviewData {
    val now: Long = LocalDateTime.of(2026, 11, 7, 10, 50).toInstant(ZoneOffset.UTC).toEpochMilli()
    private fun ago(hours: Int, minutes: Int = 0) = now - (hours * 60L + minutes) * 60_000L

    val linkItem = HistoryItem(1, HistoryKind.SCANNED, CodeKind.URL, "https://cafelumen.example/menu?table=12", "cafelumen.example", CodeFormat.QR, ScanSource.CAMERA, ago(0, 8), true)
    val shortItem = linkItem.copy(id = 2, raw = "https://short.example/x7Kq", title = "short.example", favorite = false)
    val wifiRaw = "WIFI:T:WPA;S:Lumen-Guest;P:cappuccino42;;"
    val wifiItem = HistoryItem(3, HistoryKind.SCANNED, CodeKind.WIFI, wifiRaw, "Lumen-Guest", CodeFormat.QR, ScanSource.CAMERA, ago(0, 8), false)
    val productItem = HistoryItem(4, HistoryKind.SCANNED, CodeKind.PRODUCT, "5012345678900", "5012345678900", CodeFormat.EAN_13, ScanSource.CAMERA, ago(24), false)
    val createdWifi = wifiItem.copy(id = 5, kind = HistoryKind.CREATED, source = ScanSource.CREATED, createdAt = ago(1, 35))

    val history = listOf(
        linkItem.copy(id = 11, title = "cafelumen.example", createdAt = ago(0, 8), favorite = true),
        wifiItem.copy(id = 12, createdAt = ago(1, 35)),
        productItem.copy(id = 13, title = "Oat Barista Drink 1 L", favorite = true, createdAt = ago(24)),
        linkItem.copy(id = 14, codeKind = CodeKind.TEXT, raw = "Table 12", title = "Table 12", favorite = false, createdAt = ago(25)),
        linkItem.copy(id = 15, codeKind = CodeKind.CONTACT, raw = "x", title = "Alex Moreno", favorite = false, createdAt = ago(72)),
        linkItem.copy(id = 16, codeKind = CodeKind.EVENT, raw = "x", title = "Spring Market, 12 Apr", favorite = false, createdAt = ago(73)),
    )

    fun result(item: HistoryItem, parsed: ParsedCode, link: LinkUiState? = null, product: ProductUiState? = null) =
        ResultUiState(loading = false, item = item, parsed = parsed, link = link, product = product, now = now)

    val linkResult = result(linkItem, ParsedCode.Url(linkItem.raw), LinkUiState(LinkSafety("cafelumen.example", isHttps = true, isShortened = false)))
    val shortResult = result(
        shortItem, ParsedCode.Url(shortItem.raw),
        LinkUiState(LinkSafety("short.example", isHttps = true, isShortened = true), destination = "promo-deals.example"),
    )
    val wifiResult = result(wifiItem, ParsedCode.Wifi("Lumen-Guest", WifiSecurity.WPA, "cappuccino42", false))

    private val info = ProductInfo("5012345678900", "Oat Barista Drink 1 L", "", "")
    private val offers = listOf(
        PriceOffer("FreshMart", "In store · 1.2 km", 229, "USD", true, "https://example.com"),
        PriceOffer("CornerGrocer", "In store · 0.6 km", 249, "USD", false),
        PriceOffer("MegaStore", "In store · 3.4 km", 265, "USD", false),
        PriceOffer("QuickCart", "Online · plus $3.99 shipping", 279, "USD", false),
    )
    val productResult = result(
        productItem, ParsedCode.Product(productItem.raw),
        product = ProductUiState(ProductTab.PRICES, DetailsState.Loaded(info), PricesState.Loaded(offers)),
    )
    val productOffline = result(
        productItem, ParsedCode.Product(productItem.raw),
        product = ProductUiState(ProductTab.PRICES, DetailsState.Loaded(info), PricesState.Failed(FailureReason.OFFLINE)),
    )

    val batch = listOf(
        BatchEntry(4, "cafelumen.example/menu", CodeKind.URL), BatchEntry(3, "5012345678900", CodeKind.PRODUCT),
        BatchEntry(2, "Table 12", CodeKind.TEXT), BatchEntry(1, "Lumen-Guest", CodeKind.WIFI),
    )

    val historyState = HistoryUiState(loading = false, items = history, totalCount = 128, now = now)
    val settingsState = SettingsUiState(settings = AppSettings(), privacyOptionsRequired = true, scannedCount = 128, createdCount = 6)
}
