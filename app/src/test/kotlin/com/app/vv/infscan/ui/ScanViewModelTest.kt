package com.app.vv.infscan.ui

import app.cash.turbine.test
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.DetectedCode
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.fakes.FakeBarcodeDecoder
import com.app.vv.infscan.fakes.FakeHaptics
import com.app.vv.infscan.fakes.FakeHistoryRepository
import com.app.vv.infscan.fakes.FakeSettingsRepository
import com.app.vv.infscan.fakes.MainDispatcherRule
import com.app.vv.infscan.fakes.MutableClock
import com.app.vv.infscan.fakes.RecordingAnalytics
import com.app.vv.infscan.ui.scan.CameraAccess
import com.app.vv.infscan.ui.scan.ScanEffect
import com.app.vv.infscan.ui.scan.ScanEvent
import com.app.vv.infscan.ui.scan.ScanMode
import com.app.vv.infscan.ui.scan.ScanUiState
import com.app.vv.infscan.ui.scan.ScanViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ScanViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val settings = FakeSettingsRepository()
    private val decoder = FakeBarcodeDecoder()
    private val haptics = FakeHaptics()
    private val analytics = RecordingAnalytics()
    private val clock = MutableClock()

    private fun vm() = ScanViewModel(history, settings, decoder, haptics, analytics, clock)

    private fun code(raw: String, format: CodeFormat = CodeFormat.QR) = DetectedCode(raw, format)

    @Test fun `a scanned code is saved and opens its result`() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("https://cafelumen.example/menu"))))
            val effect = awaitItem() as ScanEffect.OpenResult
            val saved = history.get(effect.id)!!
            assertEquals(CodeKind.URL, saved.codeKind)
            assertEquals("cafelumen.example", saved.title)
            assertEquals(ScanSource.CAMERA, saved.source)
        }
        assertEquals(1, haptics.ticks)
        assertTrue(AnalyticsEvents.SCAN_SUCCESS in analytics.names())
    }

    @Test fun `no vibration when the setting is off`() = runTest {
        settings.setVibrateOnScan(false)
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("hello"))))
            awaitItem()
        }
        assertEquals(0, haptics.ticks)
    }

    @Test fun `the same code is ignored for a moment and accepted later`() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("hello"))))
            awaitItem()
            clock.millis += 500
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("hello"))))
            expectNoEvents()
            clock.millis += 2_000
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("hello"))))
            awaitItem()
        }
        assertEquals(2, history.items.value.size)
    }

    @Test fun `links open straight away only when allowed and never when shortened`() = runTest {
        settings.setOpenLinksWithoutAsking(true)
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("https://cafelumen.example"))))
            assertEquals(ScanEffect.OpenUrl("https://cafelumen.example"), awaitItem())
            clock.millis += 5_000
            vm.onEvent(ScanEvent.CodesDetected(listOf(code("https://bit.ly/3abc"))))
            assertTrue(awaitItem() is ScanEffect.OpenResult)
        }
    }

    @Test fun `batch mode collects unique codes newest first`() = runTest {
        val vm = vm()
        vm.onEvent(ScanEvent.ModeSelected(ScanMode.BATCH))
        vm.onEvent(ScanEvent.CodesDetected(listOf(code("one"), code("two"))))
        vm.onEvent(ScanEvent.CodesDetected(listOf(code("one"), code("5012345678900", CodeFormat.EAN_13))))
        val batch = vm.state.value.batch
        assertEquals(listOf("5012345678900", "two", "one"), batch.map { it.value })
        assertEquals(CodeKind.PRODUCT, batch.first().kind)
        assertEquals(3, history.items.value.size)
    }

    @Test fun `batch export shares a csv of the batch only`() = runTest {
        history.seed(raw = "older", title = "older")
        val vm = vm()
        vm.onEvent(ScanEvent.ModeSelected(ScanMode.BATCH))
        vm.onEvent(ScanEvent.CodesDetected(listOf(code("one"))))
        vm.effects.test {
            vm.onEvent(ScanEvent.ExportBatch)
            val csv = (awaitItem() as ScanEffect.ShareCsv).csv
            assertTrue(csv.contains(",one"))
            assertTrue(!csv.contains("older"))
        }
    }

    @Test fun `finishing a batch resets it and opens history`() = runTest {
        val vm = vm()
        vm.onEvent(ScanEvent.ModeSelected(ScanMode.BATCH))
        vm.onEvent(ScanEvent.CodesDetected(listOf(code("one"))))
        vm.effects.test {
            vm.onEvent(ScanEvent.FinishBatch)
            assertEquals(ScanEffect.OpenHistory, awaitItem())
        }
        assertEquals(ScanMode.SCAN, vm.state.value.mode)
        assertTrue(vm.state.value.batch.isEmpty())
    }

    @Test fun `switching back to scan mode clears the batch`() = runTest {
        val vm = vm()
        vm.onEvent(ScanEvent.ModeSelected(ScanMode.BATCH))
        vm.onEvent(ScanEvent.CodesDetected(listOf(code("one"))))
        vm.onEvent(ScanEvent.ModeSelected(ScanMode.SCAN))
        assertTrue(vm.state.value.batch.isEmpty())
    }

    @Test fun `a photo without a code explains itself`() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.ImagePicked("content://photo/1"))
            assertEquals(ScanEffect.Message(R.string.scan_image_no_code), awaitItem())
        }
        assertTrue(AnalyticsEvents.SCAN_FAIL in analytics.names())
    }

    @Test fun `a photo with a code opens the result as a gallery scan`() = runTest {
        decoder.result = listOf(code("Table 12"))
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.ImagePicked("content://photo/1"))
            val id = (awaitItem() as ScanEffect.OpenResult).id
            assertEquals(ScanSource.GALLERY, history.get(id)!!.source)
        }
    }

    @Test fun `torch zoom and permission state`() = runTest {
        val vm = vm()
        vm.onEvent(ScanEvent.ToggleTorch)
        vm.onEvent(ScanEvent.CycleZoom)
        vm.onEvent(ScanEvent.AccessChanged(CameraAccess.GRANTED))
        assertTrue(vm.state.value.torchOn)
        assertEquals(2f, vm.state.value.zoomRatio, 0f)
        assertEquals(CameraAccess.GRANTED, vm.state.value.access)
        repeat(2) { vm.onEvent(ScanEvent.CycleZoom) }
        assertEquals(1f, vm.state.value.zoomRatio, 0f)
        assertTrue(AnalyticsEvents.PERMISSION_CAMERA in analytics.names())
    }

    @Test fun `the camera failure is reported once`() = runTest {
        val vm = vm()
        vm.effects.test {
            vm.onEvent(ScanEvent.CameraFailed)
            vm.onEvent(ScanEvent.CameraFailed)
            assertEquals(ScanEffect.Message(R.string.scan_camera_error), awaitItem())
            expectNoEvents()
        }
    }

    @Test fun `default state has no camera access yet`() {
        assertEquals(CameraAccess.UNKNOWN, ScanUiState().access)
        assertEquals(AppSettings.DEFAULT_RETENTION_DAYS, AppSettings().retentionDays)
    }
}
