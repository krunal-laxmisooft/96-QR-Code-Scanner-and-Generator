package com.app.vv.infscan.ui

import android.content.Context
import androidx.compose.foundation.pager.rememberPagerState
import androidx.test.core.app.ApplicationProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.app.vv.infscan.R
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.ui.preview.PreviewData as Samples
import com.app.vv.infscan.ui.create.CreateFormEvent
import com.app.vv.infscan.ui.create.CreateFormScreen
import com.app.vv.infscan.ui.create.CreateFormUiState
import com.app.vv.infscan.ui.create.CreateHomeScreen
import com.app.vv.infscan.ui.history.HistoryEvent
import com.app.vv.infscan.ui.history.HistoryFilter
import com.app.vv.infscan.ui.history.HistoryScreen
import com.app.vv.infscan.ui.history.HistoryUiState
import com.app.vv.infscan.ui.onboarding.OnboardingScreen
import com.app.vv.infscan.ui.result.ResultEvent
import com.app.vv.infscan.ui.result.ResultScreen
import com.app.vv.infscan.ui.scan.CameraAccess
import com.app.vv.infscan.ui.scan.ScanEvent
import com.app.vv.infscan.ui.scan.ScanMode
import com.app.vv.infscan.ui.scan.ScanScreen
import com.app.vv.infscan.ui.scan.ScanUiState
import com.app.vv.infscan.ui.settings.SettingsEvent
import com.app.vv.infscan.ui.settings.SettingsScreen
import com.app.vv.infscan.ui.settings.SettingsUiState
import com.app.vv.infscan.ui.theme.InfScanTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w360dp-h800dp-xhdpi")
class FlowUiTest {
    @get:Rule val compose = createComposeRule()

    private fun string(id: Int): String = ApplicationProvider.getApplicationContext<Context>().getString(id)

    // ---- Scan -> result ----

    @Test fun `scan screen asks for camera access and offers a photo fallback`() {
        val events = mutableListOf<String>()
        compose.setContent {
            InfScanTheme(darkTheme = false) {
                ScanScreen(
                    ScanUiState(access = CameraAccess.DENIED), {}, onRequestAccess = { events += "allow" }, onOpenSettings = { events += "settings" },
                    onPickImage = { events += "photo" },
                ) {}
            }
        }
        compose.onNodeWithText("Camera access is off").assertIsDisplayed()
        compose.onNodeWithText("Allow camera").performClick()
        compose.onNodeWithText("Scan from a photo instead").performClick()
        assertEquals(listOf("allow", "photo"), events)
    }

    @Test fun `permanently denied camera leads to settings`() {
        var opened = false
        compose.setContent {
            InfScanTheme(darkTheme = false) {
                ScanScreen(ScanUiState(access = CameraAccess.PERMANENTLY_DENIED), {}, {}, { opened = true }, {}) {}
            }
        }
        compose.onNodeWithText("Open settings").performClick()
        assertTrue(opened)
    }

    @Test fun `scan controls switch to batch and report events`() {
        val events = mutableListOf<ScanEvent>()
        compose.setContent {
            InfScanTheme(darkTheme = false) {
                ScanScreen(ScanUiState(access = CameraAccess.GRANTED), { events += it }, {}, {}, {}) {}
            }
        }
        compose.onNodeWithText("Point at a QR code or barcode").assertIsDisplayed()
        compose.onNodeWithText("Batch").performClick()
        compose.onNodeWithContentDescription("Turn torch on").performClick()
        compose.onNodeWithContentDescription("Zoom 1×").performClick()
        assertEquals(listOf(ScanEvent.ModeSelected(ScanMode.BATCH), ScanEvent.ToggleTorch, ScanEvent.CycleZoom), events)
    }

    @Test fun `batch panel lists scans and finishes`() {
        val events = mutableListOf<ScanEvent>()
        compose.setContent {
            InfScanTheme(darkTheme = false) {
                ScanScreen(ScanUiState(access = CameraAccess.GRANTED, mode = ScanMode.BATCH, batch = Samples.batch), { events += it }, {}, {}, {}) {}
            }
        }
        compose.onNodeWithText("Batch · 4 scanned").assertIsDisplayed()
        compose.onNodeWithText("cafelumen.example/menu").assertIsDisplayed()
        compose.onNodeWithText("CSV").performClick()
        compose.onNodeWithText("Done").performClick()
        assertEquals(listOf(ScanEvent.ExportBatch, ScanEvent.FinishBatch), events)
    }

    @Test fun `a safe link opens with one tap and shows its address`() {
        val events = mutableListOf<ResultEvent>()
        compose.setContent { InfScanTheme(darkTheme = false) { ResultScreen(Samples.linkResult, { events += it }, {}, {}) } }
        compose.onNodeWithText("cafelumen.example").assertIsDisplayed()
        compose.onNodeWithText("Secure connection (HTTPS)").assertIsDisplayed()
        compose.onNodeWithText("Open link").performClick()
        compose.onNodeWithText("Copy").performClick()
        compose.onNodeWithText("Share").performClick()
        assertEquals(listOf(ResultEvent.Primary, ResultEvent.Copy, ResultEvent.Share), events)
    }

    @Test fun `a shortened link warns before opening`() {
        val events = mutableListOf<ResultEvent>()
        compose.setContent { InfScanTheme(darkTheme = true) { ResultScreen(Samples.shortResult, { events += it }, {}, {}) } }
        compose.onNodeWithText("Shortened link").assertIsDisplayed()
        compose.onNodeWithText("It leads to promo-deals.example. Check the address before you open it.").assertIsDisplayed()
        compose.onNodeWithText("Open link").assertDoesNotExist()
        compose.onNodeWithText("Don’t open").performClick()
        compose.onNodeWithText("Open anyway").performClick()
        assertEquals(listOf(ResultEvent.DontOpen, ResultEvent.OpenAnyway), events)
    }

    @Test fun `wifi result copies the password and hides it by default`() {
        val events = mutableListOf<ResultEvent>()
        compose.setContent { InfScanTheme(darkTheme = false) { ResultScreen(Samples.wifiResult, { events += it }, {}, {}) } }
        compose.onNodeWithText("cappuccino42").assertDoesNotExist()
        compose.onNodeWithContentDescription("Show or hide password").performClick()
        compose.onNodeWithText("Copy password").performClick()
        compose.onNodeWithText("Share network").performClick()
        assertEquals(listOf(ResultEvent.TogglePassword, ResultEvent.CopyPassword, ResultEvent.Share), events)
    }

    @Test fun `product prices show the best offer and offline state offers a retry`() {
        val events = mutableListOf<ResultEvent>()
        compose.setContent { InfScanTheme(darkTheme = false) { ResultScreen(Samples.productResult, { events += it }, {}, {}) } }
        compose.onNodeWithText("Lowest nearby").assertIsDisplayed()
        compose.onNodeWithText("Open").performClick()
        assertTrue(events.single() is ResultEvent.OpenOffer)
    }

    @Test fun `offline prices explain and retry`() {
        val events = mutableListOf<ResultEvent>()
        compose.setContent { InfScanTheme(darkTheme = false) { ResultScreen(Samples.productOffline, { events += it }, {}, {}) } }
        compose.onNodeWithText("Can’t reach price sources").assertIsDisplayed()
        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Back to details").performClick()
        assertEquals(2, events.size)
        assertEquals(ResultEvent.RetryPrices, events.first())
    }

    // ---- Create ----

    @Test fun `create home lists every code type and opens a form`() {
        var picked: CreateType? = null
        compose.setContent {
            InfScanTheme(darkTheme = false) { CreateHomeScreen(null, Samples.now, { picked = it }, {}, {}) }
        }
        CreateType.entries.forEach { compose.onNodeWithText(string(it.title)).assertIsDisplayed() }
        compose.onNodeWithText("Wi-Fi").performClick()
        assertEquals(CreateType.WIFI, picked)
    }

    @Test fun `website form takes input, shows its error and submits`() {
        val events = mutableListOf<CreateFormEvent>()
        compose.setContent {
            InfScanTheme(darkTheme = false) {
                CreateFormScreen(CreateFormUiState(CreateType.WEBSITE, errors = mapOf(FormField.URL to R.string.error_url)), { events += it }, {})
            }
        }
        compose.onNodeWithText("Add a full address, like https://cafelumen.example").assertIsDisplayed()
        compose.onNodeWithText("Website").performTextInput("cafelumen.example")
        compose.onNodeWithText("Create code").performClick()
        assertEquals(CreateFormEvent.FieldChanged(FormField.URL, "cafelumen.example"), events.first())
        assertEquals(CreateFormEvent.Submit, events.last())
    }

    // ---- History ----

    private fun historyScreen(state: HistoryUiState, events: MutableList<HistoryEvent>, opened: MutableList<Long> = mutableListOf(), onScan: () -> Unit = {}) {
        compose.setContent {
            InfScanTheme(darkTheme = false) { HistoryScreen(state, { events += it }, { opened += it.id }, onScan, {}, {}) }
        }
    }

    @Test fun `history shows saved codes and opens one`() {
        val opened = mutableListOf<Long>()
        historyScreen(Samples.historyState, mutableListOf(), opened)
        compose.onNodeWithText("Lumen-Guest").assertIsDisplayed()
        compose.onNodeWithText("Oat Barista Drink 1 L").performClick()
        assertEquals(listOf(13L), opened)
    }

    @Test fun `history tabs filters search and export report events`() {
        val events = mutableListOf<HistoryEvent>()
        historyScreen(Samples.historyState, events)
        compose.onNodeWithText("Created").performClick()
        compose.onNodeWithText("Links").performClick()
        compose.onNodeWithContentDescription("Search history").performClick()
        compose.onNodeWithContentDescription("Export CSV").performClick()
        assertEquals(
            listOf(HistoryEvent.SelectTab(HistoryKind.CREATED), HistoryEvent.SelectFilter(HistoryFilter.LINKS), HistoryEvent.ToggleSearch, HistoryEvent.ExportClicked),
            events,
        )
    }

    @Test fun `favoriting a row sends the item`() {
        val events = mutableListOf<HistoryEvent>()
        historyScreen(Samples.historyState, events)
        compose.onAllNodesWithContentDescription("Add to favorites", useUnmergedTree = true)[0].performClick()
        val event = events.single() as HistoryEvent.ToggleFavorite
        assertEquals(CodeKind.WIFI, event.item.codeKind)
    }

    @Test fun `empty history invites the first scan`() {
        var scanned = false
        historyScreen(Samples.historyState.copy(items = emptyList(), totalCount = 0), mutableListOf(), onScan = { scanned = true })
        compose.onNodeWithText("Nothing scanned yet").assertIsDisplayed()
        compose.onNodeWithText("Scan a code").performClick()
        assertTrue(scanned)
    }

    @Test fun `a filter with no matches says so`() {
        historyScreen(Samples.historyState.copy(items = emptyList(), filter = HistoryFilter.WIFI), mutableListOf())
        compose.onNodeWithText("Nothing matches.").assertIsDisplayed()
    }

    // ---- Settings ----

    private fun settingsScreen(state: SettingsUiState, events: MutableList<SettingsEvent>) {
        compose.setContent {
            InfScanTheme(darkTheme = false) { SettingsScreen(state, { events += it }, {}, {}, {}, {}) }
        }
    }

    @Test fun `settings change theme and toggles`() {
        val events = mutableListOf<SettingsEvent>()
        settingsScreen(SettingsUiState(), events)
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Vibrate on scan").performClick()
        compose.onNodeWithText("Keep history").performClick()
        compose.onNodeWithText("Clear history").performClick()
        assertEquals(com.app.vv.infscan.data.prefs.ThemeMode.DARK, (events[0] as SettingsEvent.Theme).mode)
        assertEquals(listOf(SettingsEvent.ChooseRetention, SettingsEvent.ClearRequested), events.takeLast(2))
    }

    @Test fun `clear history needs a confirmation that names the counts`() {
        val events = mutableListOf<SettingsEvent>()
        settingsScreen(SettingsUiState(confirmClear = true, scannedCount = 128, createdCount = 6), events)
        compose.onNodeWithText("Clear all history?").assertIsDisplayed()
        compose.onNodeWithText("This removes 128 scans and 6 created codes from this device, favorites included. You can’t undo it.").assertIsDisplayed()
        compose.onNodeWithText("Cancel").performClick()
        compose.onAllNodesWithText("Clear history").onLast().performClick()
        assertEquals(listOf(SettingsEvent.ClearDismissed, SettingsEvent.ClearConfirmed), events)
    }

    @Test fun `ad choices appear only when consent requires them`() {
        settingsScreen(SettingsUiState(privacyOptionsRequired = false), mutableListOf())
        compose.onNodeWithText("Ad choices").assertDoesNotExist()
    }

    @Test fun `retention choices are offered`() {
        val events = mutableListOf<SettingsEvent>()
        settingsScreen(SettingsUiState(choosingRetention = true, settings = AppSettings(retentionDays = 30)), events)
        compose.onNodeWithText("90 days").performClick()
        assertEquals(SettingsEvent.Retention(90), events.single())
    }

    // ---- Onboarding ----

    @Test fun `onboarding skip and next`() {
        var next = 0
        var finished = 0
        compose.setContent {
            InfScanTheme(darkTheme = false) { OnboardingScreen(rememberPagerState(0) { 3 }, { next++ }, { finished++ }) }
        }
        compose.onNodeWithText("Scan anything, instantly").assertIsDisplayed()
        compose.onNodeWithText("Next").performClick()
        compose.onNodeWithText("Skip").performClick()
        assertEquals(1, next)
        assertEquals(1, finished)
    }

    @Test fun `last onboarding page starts scanning`() {
        var finished = 0
        compose.setContent {
            InfScanTheme(darkTheme = false) { OnboardingScreen(rememberPagerState(2) { 3 }, {}, { finished++ }) }
        }
        compose.onNodeWithText("Compare prices before you buy").assertIsDisplayed()
        compose.onNodeWithText("Start scanning").performClick()
        assertEquals(1, finished)
    }


}
