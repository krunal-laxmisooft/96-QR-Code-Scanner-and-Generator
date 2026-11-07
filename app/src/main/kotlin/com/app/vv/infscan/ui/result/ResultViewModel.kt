package com.app.vv.infscan.ui.result

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsParams
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.data.repo.HistoryRepository
import com.app.vv.infscan.data.repo.LinkResolver
import com.app.vv.infscan.data.repo.PriceRepository
import com.app.vv.infscan.data.repo.ProductRepository
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.parser.CodeParser
import com.app.vv.infscan.domain.parser.LinkSafetyChecker
import com.app.vv.infscan.ui.nav.ResultDest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

@HiltViewModel
class ResultViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val history: HistoryRepository,
    private val links: LinkResolver,
    private val products: ProductRepository,
    private val prices: PriceRepository,
    private val analytics: AnalyticsTracker,
    private val clock: Clock,
) : ViewModel() {

    private val id: Long = savedStateHandle.toRoute<ResultDest>().id

    private val _state = MutableStateFlow(ResultUiState(now = clock.millis()))
    val state: StateFlow<ResultUiState> = _state.asStateFlow()

    private val _effects = Channel<ResultEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            val item = history.get(id)
            if (item == null) {
                _state.update { it.copy(loading = false) }
                return@launch
            }
            val parsed = CodeParser.parse(item.raw, item.format)
            _state.update { it.copy(loading = false, item = item, parsed = parsed) }
            when (parsed) {
                is ParsedCode.Url -> loadLink(parsed.url)
                is ParsedCode.Product -> loadProduct(parsed.code)
                else -> Unit
            }
        }
        viewModelScope.launch {
            history.observeItem(id).collect { latest -> if (latest != null) _state.update { it.copy(item = latest) } }
        }
    }

    fun onEvent(event: ResultEvent) {
        when (event) {
            ResultEvent.ToggleFavorite -> toggleFavorite()
            ResultEvent.Copy -> copyPrimary()
            ResultEvent.CopyPassword -> (_state.value.parsed as? ParsedCode.Wifi)?.let {
                action("copy_password")
                send(ResultEffect.CopyText(it.password, R.string.result_password_copied))
            }
            ResultEvent.Share -> share()
            ResultEvent.Primary -> primary()
            ResultEvent.OpenAnyway -> (_state.value.parsed as? ParsedCode.Url)?.let { launch(ResultAction.OpenUrl(it.url), "open_anyway") }
            ResultEvent.DontOpen -> send(ResultEffect.Close)
            ResultEvent.TogglePassword -> _state.update { it.copy(passwordVisible = !it.passwordVisible) }
            is ResultEvent.SelectTab -> _state.update { s -> s.copy(product = s.product?.copy(tab = event.tab)) }
            ResultEvent.RetryPrices -> (_state.value.parsed as? ParsedCode.Product)?.let { viewModelScope.launch { loadPrices(it.code) } }
            is ResultEvent.OpenOffer -> launch(ResultAction.OpenUrl(event.url), "open_offer")
        }
    }

    private suspend fun loadLink(url: String) {
        val safety = LinkSafetyChecker.check(url)
        _state.update { it.copy(link = LinkUiState(safety, resolving = safety.isShortened)) }
        if (!safety.isShortened) return
        val destination = links.resolve(url)
        _state.update { s -> s.copy(link = s.link?.copy(resolving = false, destination = destination?.let(LinkSafetyChecker::displayHost))) }
    }

    private fun loadProduct(code: String) {
        _state.update { it.copy(product = ProductUiState(prices = if (prices.isEnabled) PricesState.Loading else PricesState.Disabled)) }
        viewModelScope.launch {
            val result = products.lookup(code)
            _state.update { s ->
                s.copy(product = s.product?.copy(details = if (result is AppResult.Success) DetailsState.Loaded(result.value) else DetailsState.Failed((result as AppResult.Failure).reason)))
            }
        }
        if (prices.isEnabled) viewModelScope.launch { loadPrices(code) }
    }

    private suspend fun loadPrices(code: String) {
        _state.update { s -> s.copy(product = s.product?.copy(prices = PricesState.Loading)) }
        val result = prices.offers(code)
        analytics.log(AnalyticsEvents.PRICE_CHECK, mapOf(AnalyticsParams.FOUND to (result is AppResult.Success && result.value.isNotEmpty())))
        _state.update { s ->
            s.copy(product = s.product?.copy(prices = if (result is AppResult.Success) PricesState.Loaded(result.value) else PricesState.Failed((result as AppResult.Failure).reason)))
        }
    }

    private fun toggleFavorite() {
        val item = _state.value.item ?: return
        viewModelScope.launch { history.setFavorite(item.id, !item.favorite) }
        action("favorite")
    }

    private fun copyPrimary() {
        val item = _state.value.item ?: return
        action("copy")
        send(ResultEffect.CopyText(copyText(item), R.string.result_copied))
    }

    private fun share() {
        val item = _state.value.item ?: return
        action("share")
        send(ResultEffect.ShareText(shareText(item)))
    }

    private fun primary() {
        when (val parsed = _state.value.parsed ?: return) {
            is ParsedCode.Url -> launch(ResultAction.OpenUrl(parsed.url), "open")
            is ParsedCode.Contact -> launch(ResultAction.AddContact(parsed), "add_contact")
            is ParsedCode.Event -> launch(ResultAction.AddEvent(parsed), "add_event")
            is ParsedCode.Sms -> launch(ResultAction.Sms(parsed.number, parsed.body), "message")
            is ParsedCode.Email -> launch(ResultAction.Email(parsed.address, parsed.subject, parsed.body), "email")
            is ParsedCode.Phone -> launch(ResultAction.Dial(parsed.number), "call")
            is ParsedCode.Location -> launch(ResultAction.Map(parsed.latitude, parsed.longitude, parsed.label), "map")
            is ParsedCode.Wifi -> onEvent(ResultEvent.CopyPassword)
            is ParsedCode.Text, is ParsedCode.Product -> copyPrimary()
        }
    }

    private fun launch(action: ResultAction, name: String) {
        action(name)
        send(ResultEffect.Launch(action))
    }

    private fun action(name: String) {
        val kind = _state.value.item?.codeKind?.name?.lowercase().orEmpty()
        analytics.log(AnalyticsEvents.RESULT_ACTION, mapOf(AnalyticsParams.ACTION to name, AnalyticsParams.TYPE to kind))
    }

    private fun send(effect: ResultEffect) {
        _effects.trySend(effect)
    }

    private fun copyText(item: HistoryItem): String = when (val parsed = _state.value.parsed) {
        is ParsedCode.Wifi -> parsed.ssid
        is ParsedCode.Url -> parsed.url
        else -> item.raw
    }

    private fun shareText(item: HistoryItem): String = when (val parsed = _state.value.parsed) {
        is ParsedCode.Wifi -> if (parsed.password.isEmpty()) parsed.ssid else "${parsed.ssid}\n${parsed.password}"
        else -> item.raw
    }
}
