package com.app.vv.infscan.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.fakes.FakeHistoryRepository
import com.app.vv.infscan.fakes.FakeImageSaver
import com.app.vv.infscan.fakes.FakeQrImages
import com.app.vv.infscan.fakes.MainDispatcherRule
import com.app.vv.infscan.fakes.MutableClock
import com.app.vv.infscan.fakes.RecordingAnalytics
import com.app.vv.infscan.ui.create.CreateFormEffect
import com.app.vv.infscan.ui.create.CreateFormEvent
import com.app.vv.infscan.ui.create.CreateFormViewModel
import com.app.vv.infscan.ui.create.CreatePreviewEffect
import com.app.vv.infscan.ui.create.CreatePreviewEvent
import com.app.vv.infscan.ui.create.CreatePreviewViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateFormViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val analytics = RecordingAnalytics()
    private val clock = MutableClock(1_700_000_000_000L)

    private fun vm(type: String) = CreateFormViewModel(SavedStateHandle(mapOf("type" to type)), history, analytics, clock)

    @Test fun `opening a form logs the start`() {
        vm("WIFI")
        assertEquals(AnalyticsEvents.CREATE_START, analytics.events.single().first)
    }

    @Test fun `an empty form shows errors and saves nothing`() = runTest {
        val vm = vm("WEBSITE")
        vm.onEvent(CreateFormEvent.Submit)
        assertEquals(R.string.error_required, vm.state.value.errors[FormField.URL])
        assertTrue(history.items.value.isEmpty())
    }

    @Test fun `typing clears that field error`() = runTest {
        val vm = vm("WEBSITE")
        vm.onEvent(CreateFormEvent.Submit)
        vm.onEvent(CreateFormEvent.FieldChanged(FormField.URL, "cafelumen"))
        assertNull(vm.state.value.errors[FormField.URL])
        vm.onEvent(CreateFormEvent.Submit)
        assertEquals(R.string.error_url, vm.state.value.errors[FormField.URL])
    }

    @Test fun `a valid wifi form is saved as a created code and opens the preview`() = runTest {
        val vm = vm("WIFI")
        vm.onEvent(CreateFormEvent.FieldChanged(FormField.SSID, "Lumen-Guest"))
        vm.onEvent(CreateFormEvent.FieldChanged(FormField.PASSWORD, "cappuccino42"))
        vm.onEvent(CreateFormEvent.HiddenChanged(true))
        vm.effects.test {
            vm.onEvent(CreateFormEvent.Submit)
            val id = (awaitItem() as CreateFormEffect.OpenPreview).id
            val saved = history.get(id)!!
            assertEquals(HistoryKind.CREATED, saved.kind)
            assertEquals(CodeKind.WIFI, saved.codeKind)
            assertEquals(ScanSource.CREATED, saved.source)
            assertEquals("WIFI:T:WPA;S:Lumen-Guest;P:cappuccino42;H:true;;", saved.raw)
            assertEquals("Lumen-Guest", saved.title)
        }
        assertTrue(AnalyticsEvents.CREATE_COMPLETE in analytics.names())
    }

    @Test fun `an open wifi network does not need a password`() = runTest {
        val vm = vm("WIFI")
        vm.onEvent(CreateFormEvent.FieldChanged(FormField.SSID, "Cafe"))
        vm.onEvent(CreateFormEvent.SecuritySelected(WifiSecurity.NONE))
        vm.effects.test {
            vm.onEvent(CreateFormEvent.Submit)
            awaitItem()
        }
        assertEquals("WIFI:T:nopass;S:Cafe;;", history.items.value.single().raw)
    }

    @Test fun `events start at the next hour and the end follows the start`() {
        val vm = vm("EVENT")
        val start = vm.state.value.values.startMillis!!
        assertTrue(start > clock.millis && start - clock.millis <= 3_600_000L)
        assertEquals(start + 3_600_000L, vm.state.value.values.endMillis)
        vm.onEvent(CreateFormEvent.StartChanged(start + 10 * 3_600_000L))
        assertEquals(start + 11 * 3_600_000L, vm.state.value.values.endMillis)
        vm.onEvent(CreateFormEvent.EndChanged(start + 12 * 3_600_000L))
        assertEquals(start + 12 * 3_600_000L, vm.state.value.values.endMillis)
    }

    @Test fun `a half filled form survives process death`() {
        val handle = SavedStateHandle(mapOf("type" to "WIFI"))
        val first = CreateFormViewModel(handle, history, analytics, clock)
        first.onEvent(CreateFormEvent.FieldChanged(FormField.SSID, "Lumen-Guest"))
        first.onEvent(CreateFormEvent.SecuritySelected(WifiSecurity.WEP))
        first.onEvent(CreateFormEvent.HiddenChanged(true))
        val restored = CreateFormViewModel(handle, history, analytics, clock).state.value.values
        assertEquals("Lumen-Guest", restored.text[FormField.SSID])
        assertEquals(WifiSecurity.WEP, restored.security)
        assertTrue(restored.hidden)
    }

    @Test fun `an event form keeps its times after restore`() {
        val handle = SavedStateHandle(mapOf("type" to "EVENT"))
        val first = CreateFormViewModel(handle, history, analytics, clock)
        first.onEvent(CreateFormEvent.StartChanged(1_800_000_000_000L))
        val restored = CreateFormViewModel(handle, history, analytics, clock).state.value.values
        assertEquals(1_800_000_000_000L, restored.startMillis)
        assertEquals(1_800_003_600_000L, restored.endMillis)
    }

    @Test fun `password visibility toggles`() {
        val vm = vm("WIFI")
        vm.onEvent(CreateFormEvent.TogglePassword)
        assertTrue(vm.state.value.passwordVisible)
    }
}

@RunWith(RobolectricTestRunner::class)
class CreatePreviewViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val saver = FakeImageSaver()
    private val analytics = RecordingAnalytics()

    private fun vm(id: Long) = CreatePreviewViewModel(SavedStateHandle(mapOf("id" to id)), history, FakeQrImages(), saver, analytics)

    private fun created() = history.seed(kind = HistoryKind.CREATED, codeKind = CodeKind.URL, raw = "https://cafelumen.example", title = "cafelumen.example")

    @Test fun `the saved code is loaded with a matrix`() {
        val state = vm(created().id).state.value
        assertNotNull(state.matrix)
        assertEquals("cafelumen.example", state.item!!.title)
    }

    @Test fun `save writes the image and reports success or failure`() = runTest {
        val vm = vm(created().id)
        vm.effects.test {
            vm.onEvent(CreatePreviewEvent.Save)
            assertEquals(CreatePreviewEffect.Saved, awaitItem())
            saver.succeeds = false
            vm.onEvent(CreatePreviewEvent.Save)
            assertEquals(CreatePreviewEffect.Message(R.string.create_save_failed), awaitItem())
        }
        assertEquals(1, saver.saved.size)
    }

    @Test fun `share hands over an image and is counted`() = runTest {
        val vm = vm(created().id)
        vm.effects.test {
            vm.onEvent(CreatePreviewEvent.Share)
            assertTrue(awaitItem() is CreatePreviewEffect.ShareImage)
        }
        assertTrue(AnalyticsEvents.CREATE_SHARE in analytics.names())
    }

    @Test fun `copy returns the payload`() = runTest {
        val vm = vm(created().id)
        vm.effects.test {
            vm.onEvent(CreatePreviewEvent.Copy)
            assertEquals(CreatePreviewEffect.CopyText("https://cafelumen.example"), awaitItem())
        }
    }

    @Test fun `undo removes the code from history and closes`() = runTest {
        val item = created()
        val vm = vm(item.id)
        vm.effects.test {
            vm.onEvent(CreatePreviewEvent.Undo)
            assertEquals(CreatePreviewEffect.Closed, awaitItem())
        }
        assertTrue(history.items.value.isEmpty())
    }
}
