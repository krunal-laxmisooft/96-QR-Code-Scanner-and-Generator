package com.app.vv.infscan.ui.result

import androidx.annotation.StringRes
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.repo.PriceOffer
import com.app.vv.infscan.data.repo.ProductInfo
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.parser.LinkSafety

enum class ProductTab { DETAILS, PRICES }

data class LinkUiState(
    val safety: LinkSafety,
    val resolving: Boolean = false,
    val destination: String? = null,
) {
    val needsWarning: Boolean get() = !safety.isHttps || safety.isShortened
}

sealed interface DetailsState {
    data object Loading : DetailsState
    data class Loaded(val info: ProductInfo) : DetailsState
    data class Failed(val reason: FailureReason) : DetailsState
}

sealed interface PricesState {
    data object Disabled : PricesState
    data object Loading : PricesState
    data class Loaded(val offers: List<PriceOffer>) : PricesState
    data class Failed(val reason: FailureReason) : PricesState
}

data class ProductUiState(
    val tab: ProductTab = ProductTab.DETAILS,
    val details: DetailsState = DetailsState.Loading,
    val prices: PricesState = PricesState.Loading,
)

data class ResultUiState(
    val loading: Boolean = true,
    val item: HistoryItem? = null,
    val parsed: ParsedCode? = null,
    val link: LinkUiState? = null,
    val product: ProductUiState? = null,
    val passwordVisible: Boolean = false,
    val now: Long = 0L,
)

sealed interface ResultEvent {
    data object ToggleFavorite : ResultEvent
    data object Copy : ResultEvent
    data object CopyPassword : ResultEvent
    data object Share : ResultEvent
    data object Primary : ResultEvent
    data object OpenAnyway : ResultEvent
    data object DontOpen : ResultEvent
    data object TogglePassword : ResultEvent
    data class SelectTab(val tab: ProductTab) : ResultEvent
    data object RetryPrices : ResultEvent
    data class OpenOffer(val url: String) : ResultEvent
}

sealed interface ResultAction {
    data class OpenUrl(val url: String) : ResultAction
    data class Dial(val number: String) : ResultAction
    data class Sms(val number: String, val body: String) : ResultAction
    data class Email(val address: String, val subject: String, val body: String) : ResultAction
    data class Map(val latitude: Double, val longitude: Double, val label: String) : ResultAction
    data class AddContact(val contact: ParsedCode.Contact) : ResultAction
    data class AddEvent(val event: ParsedCode.Event) : ResultAction
}

sealed interface ResultEffect {
    data class Launch(val action: ResultAction) : ResultEffect
    data class ShareText(val text: String) : ResultEffect
    data class CopyText(val text: String, @StringRes val message: Int) : ResultEffect
    data object Close : ResultEffect
    data class Message(@StringRes val text: Int) : ResultEffect
}
