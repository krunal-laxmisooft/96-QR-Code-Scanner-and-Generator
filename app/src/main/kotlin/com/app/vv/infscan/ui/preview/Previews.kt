package com.app.vv.infscan.ui.preview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewFontScale
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.ui.components.ActionTile
import com.app.vv.infscan.ui.components.AdBannerFrame
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfBottomNav
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfCard
import com.app.vv.infscan.ui.components.InfChip
import com.app.vv.infscan.ui.components.InfDialogContent
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.components.InfTextField
import com.app.vv.infscan.ui.components.InfTopBar
import com.app.vv.infscan.ui.components.MainTab
import com.app.vv.infscan.ui.components.NativeAdCardFrame
import com.app.vv.infscan.ui.components.SegmentedControl
import com.app.vv.infscan.ui.components.TypeChip
import com.app.vv.infscan.ui.create.CreateFormScreen
import com.app.vv.infscan.ui.create.CreateFormUiState
import com.app.vv.infscan.ui.create.CreateHomeScreen
import com.app.vv.infscan.ui.history.HistoryScreen
import com.app.vv.infscan.ui.onboarding.OnboardingScreen
import com.app.vv.infscan.ui.result.NoticeBanner
import com.app.vv.infscan.ui.result.NoticeTone
import com.app.vv.infscan.ui.result.ResultScreen
import com.app.vv.infscan.ui.scan.CameraAccess
import com.app.vv.infscan.ui.scan.ScanScreen
import com.app.vv.infscan.ui.scan.ScanUiState
import com.app.vv.infscan.ui.settings.SettingsScreen
import com.app.vv.infscan.ui.theme.InfScanTheme
import com.app.vv.infscan.ui.theme.InfTheme

/** Light, dark and every font scale, so each component and screen is checked in all three. */
@PreviewLightDark
@PreviewFontScale
annotation class InfPreviews

@InfPreviews
@Composable
private fun ButtonsPreview() = InfScanTheme {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        InfButton("Open link", {}, icon = R.drawable.ic_open_in_new)
        InfButton("Share", {}, kind = ButtonKind.Tonal, icon = R.drawable.ic_share)
        InfButton("Cancel", {}, kind = ButtonKind.Outline)
        InfButton("Not now", {}, kind = ButtonKind.Text)
        InfButton("Clear history", {}, kind = ButtonKind.Danger)
        InfButton("Unavailable", {}, enabled = false)
        ActionTile("Copy", R.drawable.ic_copy, {})
    }
}

@InfPreviews
@Composable
private fun ControlsPreview() = InfScanTheme {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            InfChip("All", true, {})
            InfChip("Links", false, {})
        }
        TypeChip(R.drawable.ic_link, "Website")
        InfTextField("Website", "cafelumen", {}, error = "Add a full address, like https://cafelumen.example")
        SegmentedControl(listOf("System", "Light", "Dark"), "System", { it }, {})
        NoticeBanner(R.drawable.ic_shield, "Secure connection (HTTPS)", "InfScan never opens a link for you.", NoticeTone.Success)
        NoticeBanner(R.drawable.ic_warning, "Shortened link", "It leads to promo-deals.example.", NoticeTone.Warning)
    }
}

@InfPreviews
@Composable
private fun BarsPreview() = InfScanTheme {
    Column {
        InfTopBar("History", onBack = {}, backLabel = "Back")
        InfBottomNav(MainTab.HISTORY, {})
    }
}

@InfPreviews
@Composable
private fun CardsAndAdsPreview() = InfScanTheme {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InfCard { TypeChip(R.drawable.ic_wifi, "Wi-Fi network") }
        AdBannerFrame(text = "Northwind Audio · Free shipping this week")
        NativeAdCardFrame("Northwind Audio", "Earbuds with a 30-hour battery", "Free shipping on orders this week.", "Shop now") {
            InfIcon(R.drawable.ic_image, null, tint = InfTheme.colors.inkSecondary)
        }
        InfDialogContent(R.drawable.ic_delete, InfTheme.colors.error, "Clear all history?", "This removes 128 scans from this device.", "Cancel", "Clear history", {}, {})
    }
}

@InfPreviews
@Composable
private fun OnboardingPreview() = InfScanTheme { OnboardingScreen(rememberPagerState(2) { 3 }, {}, {}) }

@InfPreviews
@Composable
private fun ScanDeniedPreview() = InfScanTheme { ScanScreen(ScanUiState(access = CameraAccess.DENIED), {}, {}, {}, {}) {} }

@InfPreviews
@Composable
private fun ResultLinkPreview() = InfScanTheme { ResultScreen(PreviewData.linkResult, {}, {}, {}) }

@InfPreviews
@Composable
private fun ResultWarningPreview() = InfScanTheme { ResultScreen(PreviewData.shortResult, {}, {}, {}) }

@InfPreviews
@Composable
private fun ResultProductPreview() = InfScanTheme { ResultScreen(PreviewData.productResult, {}, {}, {}) }

@InfPreviews
@Composable
private fun CreateHomePreview() = InfScanTheme { CreateHomeScreen(PreviewData.createdWifi, PreviewData.now, {}, {}, {}) }

@InfPreviews
@Composable
private fun CreateFormPreview() = InfScanTheme { CreateFormScreen(CreateFormUiState(CreateType.WIFI), {}, {}) }

@InfPreviews
@Composable
private fun HistoryPreview() = InfScanTheme { HistoryScreen(PreviewData.historyState, {}, {}, {}, {}, {}) }

@InfPreviews
@Composable
private fun SettingsPreview() = InfScanTheme { SettingsScreen(PreviewData.settingsState, {}, {}, {}, {}, {}) }
