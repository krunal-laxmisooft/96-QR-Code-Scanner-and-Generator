package com.app.vv.infscan.ui

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.app.vv.infscan.R
import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.repo.PriceOffer
import com.app.vv.infscan.data.repo.ProductInfo
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.fakes.FakeHistoryRepository
import com.app.vv.infscan.fakes.FakeLinkResolver
import com.app.vv.infscan.fakes.FakePrices
import com.app.vv.infscan.fakes.FakeProductRepository
import com.app.vv.infscan.fakes.MainDispatcherRule
import com.app.vv.infscan.fakes.MutableClock
import com.app.vv.infscan.fakes.RecordingAnalytics
import com.app.vv.infscan.ui.result.DetailsState
import com.app.vv.infscan.ui.result.PricesState
import com.app.vv.infscan.ui.result.ProductTab
import com.app.vv.infscan.ui.result.ResultAction
import com.app.vv.infscan.ui.result.ResultEffect
import com.app.vv.infscan.ui.result.ResultEvent
import com.app.vv.infscan.ui.result.ResultViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ResultViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    private val history = FakeHistoryRepository()
    private val links = FakeLinkResolver()
    private val products = FakeProductRepository()
    private val prices = FakePrices()
    private val analytics = RecordingAnalytics()

    private fun vm(id: Long) = ResultViewModel(SavedStateHandle(mapOf("id" to id)), history, links, products, prices, analytics, MutableClock())

    @Test fun `a missing item stops loading without content`() {
        val state = vm(99).state.value
        assertFalse(state.loading)
        assertNull(state.item)
    }

    @Test fun `a secure link shows no warning and opens`() = runTest {
        val item = history.seed(raw = "https://cafelumen.example/menu", title = "cafelumen.example")
        val vm = vm(item.id)
        val link = vm.state.value.link!!
        assertTrue(link.safety.isHttps)
        assertFalse(link.needsWarning)
        vm.effects.test {
            vm.onEvent(ResultEvent.Primary)
            assertEquals(ResultEffect.Launch(ResultAction.OpenUrl("https://cafelumen.example/menu")), awaitItem())
        }
        assertEquals("open", analytics.events.last().second["action"])
    }

    @Test fun `a shortened link is resolved and warns`() = runTest {
        links.destination = "https://promo-deals.example/win-prize"
        val bit = history.seed(raw = "https://bit.ly/3abc", title = "bit.ly")
        val state = vm(bit.id).state.value.link!!
        assertTrue(state.needsWarning)
        assertEquals("promo-deals.example", state.destination)
        assertFalse(state.resolving)
    }

    @Test fun `an unresolvable short link still warns`() = runTest {
        val item = history.seed(raw = "https://bit.ly/3abc", title = "bit.ly")
        val link = vm(item.id).state.value.link!!
        assertNull(link.destination)
        assertTrue(link.needsWarning)
    }

    @Test fun `plain http warns`() = runTest {
        val item = history.seed(raw = "http://cafelumen.example", title = "cafelumen.example")
        assertTrue(vm(item.id).state.value.link!!.needsWarning)
    }

    @Test fun `dont open closes and open anyway launches`() = runTest {
        val item = history.seed(raw = "http://cafelumen.example", title = "x")
        val vm = vm(item.id)
        vm.effects.test {
            vm.onEvent(ResultEvent.DontOpen)
            assertEquals(ResultEffect.Close, awaitItem())
            vm.onEvent(ResultEvent.OpenAnyway)
            assertEquals(ResultEffect.Launch(ResultAction.OpenUrl("http://cafelumen.example")), awaitItem())
        }
    }

    @Test fun `copy and share use the code content`() = runTest {
        val item = history.seed(raw = "https://cafelumen.example", title = "x")
        val vm = vm(item.id)
        vm.effects.test {
            vm.onEvent(ResultEvent.Copy)
            assertEquals(ResultEffect.CopyText("https://cafelumen.example", R.string.result_copied), awaitItem())
            vm.onEvent(ResultEvent.Share)
            assertEquals(ResultEffect.ShareText("https://cafelumen.example"), awaitItem())
        }
    }

    @Test fun `wifi copies the password and shares the network`() = runTest {
        val item = history.seed(codeKind = CodeKind.WIFI, raw = "WIFI:T:WPA;S:Lumen-Guest;P:cappuccino42;;", title = "Lumen-Guest")
        val vm = vm(item.id)
        vm.effects.test {
            vm.onEvent(ResultEvent.CopyPassword)
            assertEquals(ResultEffect.CopyText("cappuccino42", R.string.result_password_copied), awaitItem())
            vm.onEvent(ResultEvent.Share)
            assertEquals(ResultEffect.ShareText("Lumen-Guest\ncappuccino42"), awaitItem())
        }
        vm.onEvent(ResultEvent.TogglePassword)
        assertTrue(vm.state.value.passwordVisible)
    }

    @Test fun `favorite toggles in the repository and the state`() = runTest {
        val item = history.seed()
        val vm = vm(item.id)
        vm.onEvent(ResultEvent.ToggleFavorite)
        assertTrue(vm.state.value.item!!.favorite)
        vm.onEvent(ResultEvent.ToggleFavorite)
        assertFalse(vm.state.value.item!!.favorite)
    }

    @Test fun `type specific primary actions`() = runTest {
        suspend fun primary(raw: String): ResultAction {
            val item = history.seed(raw = raw, title = raw)
            var action: ResultAction? = null
            val vm = vm(item.id)
            vm.effects.test {
                vm.onEvent(ResultEvent.Primary)
                action = (awaitItem() as ResultEffect.Launch).action
            }
            return action!!
        }
        assertEquals(ResultAction.Dial("+15550100"), primary("tel:+15550100"))
        assertEquals(ResultAction.Sms("5550100", "Hi"), primary("SMSTO:5550100:Hi"))
        assertEquals(ResultAction.Email("a@b.example", "S", "B"), primary("mailto:a@b.example?subject=S&body=B"))
        assertEquals(ResultAction.Map(1.0, 2.0, ""), primary("geo:1,2"))
    }

    @Test fun `product details and prices load`() = runTest {
        products.result = AppResult.Success(ProductInfo("5012345678900", "Oat Barista Drink 1 L", "Oatly", "1 L"))
        prices.result = AppResult.Success(listOf(PriceOffer("FreshMart", "In store", 229, "USD", true)))
        val item = history.seed(codeKind = CodeKind.PRODUCT, raw = "5012345678900", title = "5012345678900", format = CodeFormat.EAN_13)
        val vm = vm(item.id)
        val product = vm.state.value.product!!
        assertEquals("Oat Barista Drink 1 L", (product.details as DetailsState.Loaded).info.name)
        assertEquals(1, (product.prices as PricesState.Loaded).offers.size)
        vm.onEvent(ResultEvent.SelectTab(ProductTab.PRICES))
        assertEquals(ProductTab.PRICES, vm.state.value.product!!.tab)
        assertEquals(true, analytics.events.last { it.first == "price_check" }.second["found"])
    }

    @Test fun `price check can be disabled and retried after failing`() = runTest {
        prices.isEnabled = false
        val off = history.seed(codeKind = CodeKind.PRODUCT, raw = "111", title = "111", format = CodeFormat.EAN_13)
        assertEquals(PricesState.Disabled, vm(off.id).state.value.product!!.prices)
        assertEquals(0, prices.calls)

        prices.isEnabled = true
        prices.result = AppResult.Failure(FailureReason.OFFLINE)
        val on = history.seed(codeKind = CodeKind.PRODUCT, raw = "222", title = "222", format = CodeFormat.EAN_13)
        val vm = vm(on.id)
        assertEquals(PricesState.Failed(FailureReason.OFFLINE), vm.state.value.product!!.prices)
        prices.result = AppResult.Success(listOf(PriceOffer("FreshMart", "In store", 229, "USD", true)))
        vm.onEvent(ResultEvent.RetryPrices)
        assertTrue(vm.state.value.product!!.prices is PricesState.Loaded)
    }

    @Test fun `missing product details are reported`() = runTest {
        val item = history.seed(codeKind = CodeKind.PRODUCT, raw = "333", title = "333", format = CodeFormat.EAN_13)
        val details = vm(item.id).state.value.product!!.details
        assertEquals(DetailsState.Failed(FailureReason.NOT_FOUND), details)
    }
}
