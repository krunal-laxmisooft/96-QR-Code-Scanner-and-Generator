package com.app.vv.infscan.parity

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.create.FormValues
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.platform.QrEncoder
import com.app.vv.infscan.ui.preview.PreviewData as Samples
import com.app.vv.infscan.ui.components.AdBannerFrame
import com.app.vv.infscan.ui.components.InfBottomNav
import com.app.vv.infscan.ui.components.InfDialogContent
import com.app.vv.infscan.ui.components.InfSnackbarHost
import com.app.vv.infscan.ui.components.MainTab
import com.app.vv.infscan.ui.components.NativeAdCardFrame
import com.app.vv.infscan.ui.create.CreateFormScreen
import com.app.vv.infscan.ui.create.CreateFormUiState
import com.app.vv.infscan.ui.create.CreateHomeScreen
import com.app.vv.infscan.ui.create.CreatePreviewScreen
import com.app.vv.infscan.ui.create.CreatePreviewUiState
import com.app.vv.infscan.ui.history.ExportSheetContent
import com.app.vv.infscan.ui.history.HistoryScreen
import com.app.vv.infscan.ui.onboarding.OnboardingScreen
import com.app.vv.infscan.ui.result.ResultScreen
import com.app.vv.infscan.ui.scan.CameraAccess
import com.app.vv.infscan.ui.scan.ScanMode
import com.app.vv.infscan.ui.scan.ScanScreen
import com.app.vv.infscan.ui.scan.ScanUiState
import com.app.vv.infscan.ui.settings.SettingsScreen
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes

class Scene(val name: String, val dark: Boolean = false, val content: @Composable () -> Unit)

private const val BANNER_TEXT = "Northwind Audio · Free shipping this week"

@Composable
private fun SampleNativeAd() {
    NativeAdCardFrame("Northwind Audio", "Earbuds with a 30-hour battery", "Free shipping on orders this week.", "Shop now") {
        com.app.vv.infscan.ui.components.InfIcon(R.drawable.ic_image, null, tint = InfTheme.colors.inkSecondary, size = 28.dp)
    }
}

@Composable
private fun PosterMock(scope: BoxScope) {
    with(scope) {
        val c = InfTheme.colors
        Box(
            Modifier.offset(62.dp, 96.dp).size(236.dp, 306.dp).rotate(-3f).background(c.paper, androidx.compose.foundation.shape.RoundedCornerShape(6.dp)),
        ) {
            Text("Café Lumen", style = InfTheme.type.titleMedium.copy(fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)), color = androidx.compose.ui.graphics.Color(0xFF2B2B2B), modifier = Modifier.align(Alignment.TopCenter).offset(y = 18.dp))
            val m = remember { QrEncoder.matrix("https://cafelumen.example/menu") }
            Canvas(Modifier.align(Alignment.TopCenter).offset(y = 56.dp).size(150.dp)) {
                val cell = size.width / (m.size + 4)
                for (y in 0 until m.size) for (x in 0 until m.size) if (m[x, y]) drawRect(androidx.compose.ui.graphics.Color(0xFF1A1A1A), Offset((x + 2) * cell, (y + 2) * cell), Size(cell + 0.5f, cell + 0.5f))
            }
            Text("Scan for today’s menu", style = InfTheme.type.caption, color = androidx.compose.ui.graphics.Color(0xFF444444), modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-22).dp))
        }
    }
}

@Composable
private fun WithNav(tab: MainTab, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) { content() }
        InfBottomNav(tab, {})
    }
}

@Composable
private fun Overlay(base: @Composable () -> Unit, overlay: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize()) {
        base()
        Box(Modifier.fillMaxSize().background(InfTheme.colors.scrim))
        overlay()
    }
}

private fun wifiForm() = CreateFormUiState(
    CreateType.WIFI,
    FormValues(text = mapOf(FormField.SSID to "Lumen-Guest", FormField.PASSWORD to "cappuccino42")),
    passwordVisible = true,
)

private fun createdPreview() = CreatePreviewUiState(
    item = Samples.createdWifi,
    parsed = ParsedCode.Wifi("Lumen-Guest", com.app.vv.infscan.domain.model.WifiSecurity.WPA, "cappuccino42", false),
    matrix = QrEncoder.matrix("WIFI:T:WPA;S:Lumen-Guest;P:cappuccino42;;"),
    loading = false,
)

private val granted = ScanUiState(access = CameraAccess.GRANTED)

@Composable
private fun ScanScene(state: ScanUiState) {
    ScanScreen(state, {}, {}, {}, {}) { PosterMock(this) }
}

val scenes: List<Scene> = listOf(
    Scene("S02-Onboarding-1") { OnboardingScreen(rememberPagerState(0) { 3 }, {}, {}) },
    Scene("S03-Onboarding-2") { OnboardingScreen(rememberPagerState(1) { 3 }, {}, {}) },
    Scene("S04-Onboarding-3") { OnboardingScreen(rememberPagerState(2) { 3 }, {}, {}) },
    Scene("S05-Scan") { WithNav(MainTab.SCAN) { ScanScene(granted) } },
    Scene("S06-Scan-Camera-Denied") { WithNav(MainTab.SCAN) { ScanScreen(ScanUiState(access = CameraAccess.PERMANENTLY_DENIED), {}, {}, {}, {}) {} } },
    Scene("S07-Scan-Batch") { WithNav(MainTab.SCAN) { ScanScene(granted.copy(mode = ScanMode.BATCH, batch = Samples.batch)) } },
    Scene("S08-Result-Link") { ResultScreen(Samples.linkResult, {}, {}, { SampleNativeAd() }) },
    Scene("S09-Result-Product") { ResultScreen(Samples.productResult, {}, {}, { SampleNativeAd() }) },
    Scene("S10-Result-WiFi") { ResultScreen(Samples.wifiResult, {}, {}, { SampleNativeAd() }) },
    Scene("S11-Create") {
        WithNav(MainTab.CREATE) { CreateHomeScreen(Samples.createdWifi.copy(title = "Lumen-Guest", createdAt = Samples.now - 60_000L), Samples.now, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }) }
    },
    Scene("S12-Create-WiFi") { CreateFormScreen(wifiForm(), {}, {}) },
    Scene("S13-Create-Preview") {
        val host = remember { SnackbarHostState() }
        LaunchedEffect(Unit) { host.showSnackbar("Saved to History", "Undo", duration = SnackbarDuration.Indefinite) }
        Box(Modifier.fillMaxSize()) {
            CreatePreviewScreen(createdPreview(), {}, {})
            InfSnackbarHost(host, Modifier.align(Alignment.BottomCenter).offset(y = (-20).dp))
        }
    },
    Scene("S14-History") {
        WithNav(MainTab.HISTORY) { HistoryScreen(Samples.historyState, {}, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }, {}) }
    },
    Scene("S15-History-Empty") {
        WithNav(MainTab.HISTORY) { HistoryScreen(Samples.historyState.copy(items = emptyList(), totalCount = 0), {}, {}, {}, {}, {}) }
    },
    Scene("S16-Settings") { WithNav(MainTab.SETTINGS) { SettingsScreen(Samples.settingsState, {}, {}, {}, {}, {}) } },
    Scene("S17-Export-Sheet") {
        Overlay({ WithNav(MainTab.HISTORY) { HistoryScreen(Samples.historyState, {}, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }, {}) } }) {
            androidx.compose.foundation.layout.Column(
                Modifier.align(Alignment.BottomCenter).background(InfTheme.colors.surface, Shapes.sheet).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 24.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(36.dp, 4.dp).background(InfTheme.colors.outline, Shapes.pill))
                Box(Modifier.size(1.dp, 10.dp))
                ExportSheetContent(128, {}, {}, {})
            }
        }
    },
    Scene("S18-Clear-Dialog") {
        Overlay({ WithNav(MainTab.HISTORY) { HistoryScreen(Samples.historyState, {}, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }, {}) } }) {
            InfDialogContent(
                R.drawable.ic_delete, InfTheme.colors.error, "Clear all history?",
                "This removes 128 scans and 6 created codes from this device, favorites included. You can’t undo it.",
                "Cancel", "Clear history", {}, {}, Modifier.align(Alignment.TopCenter).offset(y = 250.dp).size(296.dp, androidx.compose.ui.unit.Dp.Unspecified),
            )
        }
    },
    Scene("S19-Price-Offline") { ResultScreen(Samples.productOffline, {}, {}, {}) },
    Scene("S20-Dark-Scan", dark = true) { WithNav(MainTab.SCAN) { ScanScene(granted) } },
    Scene("S21-Dark-Result-Warning", dark = true) { ResultScreen(Samples.shortResult, {}, {}, { SampleNativeAd() }) },
    Scene("S22-Dark-History", dark = true) {
        WithNav(MainTab.HISTORY) { HistoryScreen(Samples.historyState, {}, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }, {}) }
    },
    Scene("S23-Dark-Create", dark = true) {
        WithNav(MainTab.CREATE) { CreateHomeScreen(Samples.createdWifi.copy(title = "Lumen-Guest", createdAt = Samples.now - 60_000L), Samples.now, {}, {}, { AdBannerFrame(text = BANNER_TEXT) }) }
    },
)
