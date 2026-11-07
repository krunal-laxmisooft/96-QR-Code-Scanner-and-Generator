package com.app.vv.infscan.ui

import app.cash.turbine.test
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.fakes.FakeAdManager
import com.app.vv.infscan.fakes.FakeHistoryRepository
import com.app.vv.infscan.fakes.FakeSettingsRepository
import com.app.vv.infscan.fakes.MainDispatcherRule
import com.app.vv.infscan.fakes.MutableClock
import com.app.vv.infscan.fakes.RecordingAnalytics
import com.app.vv.infscan.ui.history.HistoryEffect
import com.app.vv.infscan.ui.history.HistoryEvent
import com.app.vv.infscan.ui.history.HistoryFilter
import com.app.vv.infscan.ui.history.HistoryViewModel
import com.app.vv.infscan.ui.settings.SettingsEvent
import com.app.vv.infscan.ui.settings.SettingsViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val analytics = RecordingAnalytics()

    private fun vm() = HistoryViewModel(history, analytics, MutableClock(10_000L))

    private fun kotlinx.coroutines.test.TestScope.watch(vm: HistoryViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
    }

    @Test fun `scanned and created codes live on separate tabs`() = runTest {
        history.seed(title = "scanned", createdAt = 1)
        history.seed(kind = HistoryKind.CREATED, title = "created", createdAt = 2)
        val vm = vm().also { watch(it) }
        assertEquals(listOf("scanned"), vm.state.value.items.map { it.title })
        vm.onEvent(HistoryEvent.SelectTab(HistoryKind.CREATED))
        assertEquals(listOf("created"), vm.state.value.items.map { it.title })
        assertEquals(2, vm.state.value.totalCount)
    }

    @Test fun `filters and search narrow the list`() = runTest {
        history.seed(codeKind = CodeKind.URL, title = "cafelumen.example", raw = "https://cafelumen.example", createdAt = 3)
        history.seed(codeKind = CodeKind.WIFI, title = "Lumen-Guest", raw = "WIFI:S:Lumen-Guest;;", createdAt = 2)
        history.seed(codeKind = CodeKind.PRODUCT, title = "Oat drink", raw = "5012345678900", createdAt = 1)
        val vm = vm().also { watch(it) }
        assertEquals(3, vm.state.value.items.size)
        vm.onEvent(HistoryEvent.SelectFilter(HistoryFilter.WIFI))
        assertEquals(listOf("Lumen-Guest"), vm.state.value.items.map { it.title })
        vm.onEvent(HistoryEvent.SelectFilter(HistoryFilter.ALL))
        vm.onEvent(HistoryEvent.QueryChanged("5012"))
        assertEquals(listOf("Oat drink"), vm.state.value.items.map { it.title })
        assertTrue(vm.state.value.isFiltered)
        vm.onEvent(HistoryEvent.QueryChanged("nothing"))
        assertTrue(vm.state.value.items.isEmpty())
    }

    @Test fun `closing search clears the query`() = runTest {
        val vm = vm().also { watch(it) }
        vm.onEvent(HistoryEvent.ToggleSearch)
        vm.onEvent(HistoryEvent.QueryChanged("abc"))
        assertTrue(vm.state.value.searching)
        vm.onEvent(HistoryEvent.ToggleSearch)
        assertFalse(vm.state.value.searching)
        assertEquals("", vm.state.value.query)
    }

    @Test fun `favorite delete and restore`() = runTest {
        val item = history.seed(title = "one")
        val vm = vm().also { watch(it) }
        vm.onEvent(HistoryEvent.ToggleFavorite(item))
        assertTrue(vm.state.value.items.single().favorite)
        vm.effects.test {
            vm.onEvent(HistoryEvent.Delete(item))
            val deleted = (awaitItem() as HistoryEffect.Deleted).item
            assertTrue(vm.state.value.items.isEmpty())
            vm.onEvent(HistoryEvent.Restore(deleted))
        }
        assertEquals(1, vm.state.value.items.size)
    }

    @Test fun `small histories export straight away`() = runTest {
        repeat(3) { history.seed(title = "item $it", createdAt = it.toLong()) }
        val vm = vm().also { watch(it) }
        vm.effects.test {
            vm.onEvent(HistoryEvent.ExportClicked)
            val csv = (awaitItem() as HistoryEffect.ShareCsv).csv
            assertEquals(4, csv.lines().filter { it.isNotBlank() }.size)
        }
        assertNull(vm.state.value.exportSheetCount)
    }

    @Test fun `large histories offer a video or the latest fifty`() = runTest {
        repeat(60) { history.seed(title = "item $it", createdAt = it.toLong()) }
        val vm = vm().also { watch(it) }
        vm.onEvent(HistoryEvent.ExportClicked)
        assertEquals(60, vm.state.value.exportSheetCount)
        vm.effects.test {
            vm.onEvent(HistoryEvent.ExportLatest)
            val rows = (awaitItem() as HistoryEffect.ShareCsv).csv.lines().filter { it.isNotBlank() }
            assertEquals(51, rows.size)
            assertTrue(rows[1].contains("item 59"))
            vm.onEvent(HistoryEvent.ExportClicked)
            vm.onEvent(HistoryEvent.WatchVideoToExport)
            assertEquals(HistoryEffect.ShowRewardedThenExport, awaitItem())
            vm.onEvent(HistoryEvent.ExportAll)
            assertEquals(61, (awaitItem() as HistoryEffect.ShareCsv).csv.lines().filter { it.isNotBlank() }.size)
        }
        assertNull(vm.state.value.exportSheetCount)
    }

    @Test fun `an empty history has nothing to export and dismissing hides the sheet`() = runTest {
        val vm = vm().also { watch(it) }
        vm.effects.test {
            vm.onEvent(HistoryEvent.ExportClicked)
            expectNoEvents()
        }
        repeat(51) { history.seed() }
        vm.onEvent(HistoryEvent.ExportClicked)
        vm.onEvent(HistoryEvent.DismissExport)
        assertNull(vm.state.value.exportSheetCount)
    }
}

class SettingsViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val settings = FakeSettingsRepository()
    private val history = FakeHistoryRepository()
    private val ads = FakeAdManager()

    private fun kotlinx.coroutines.test.TestScope.vm(): SettingsViewModel {
        val vm = SettingsViewModel(settings, history, ads)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        return vm
    }

    @Test fun `settings changes are stored`() = runTest {
        val vm = vm()
        vm.onEvent(SettingsEvent.Theme(ThemeMode.DARK))
        vm.onEvent(SettingsEvent.Vibrate(false))
        vm.onEvent(SettingsEvent.OpenLinks(true))
        val s = vm.state.value.settings
        assertEquals(ThemeMode.DARK, s.themeMode)
        assertFalse(s.vibrateOnScan)
        assertTrue(s.openLinksWithoutAsking)
    }

    @Test fun `retention dialog saves a choice and closes`() = runTest {
        val vm = vm()
        vm.onEvent(SettingsEvent.ChooseRetention)
        assertTrue(vm.state.value.choosingRetention)
        vm.onEvent(SettingsEvent.Retention(AppSettings.KEEP_FOREVER))
        assertFalse(vm.state.value.choosingRetention)
        assertEquals(AppSettings.KEEP_FOREVER, vm.state.value.settings.retentionDays)
    }

    @Test fun `clearing history asks first and then removes everything`() = runTest {
        history.seed()
        history.seed(kind = HistoryKind.CREATED)
        val vm = vm()
        vm.onEvent(SettingsEvent.ClearRequested)
        assertTrue(vm.state.value.confirmClear)
        assertEquals(1, vm.state.value.scannedCount)
        assertEquals(1, vm.state.value.createdCount)
        vm.onEvent(SettingsEvent.ClearDismissed)
        assertEquals(2, history.items.value.size)
        vm.onEvent(SettingsEvent.ClearRequested)
        vm.onEvent(SettingsEvent.ClearConfirmed)
        assertTrue(history.items.value.isEmpty())
        assertFalse(vm.state.value.confirmClear)
    }

    @Test fun `ad choices follow consent requirements`() = runTest {
        val vm = vm()
        assertFalse(vm.state.value.privacyOptionsRequired)
        ads.privacyOptionsRequired.value = true
        assertTrue(vm.state.value.privacyOptionsRequired)
    }
}
