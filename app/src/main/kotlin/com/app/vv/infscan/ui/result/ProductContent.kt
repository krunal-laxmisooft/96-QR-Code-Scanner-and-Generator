package com.app.vv.infscan.ui.result

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.repo.PriceOffer
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.ui.common.label
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.components.InfIcon
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space
import java.text.NumberFormat
import java.util.Currency

@Composable
fun ProductContent(item: HistoryItem, parsed: ParsedCode.Product, state: ResultUiState, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    val product = state.product ?: ProductUiState()
    val info = (product.details as? DetailsState.Loaded)?.info
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(72.dp).clip(Shapes.card).background(c.surfaceVariant), contentAlignment = Alignment.Center) {
            InfIcon(R.drawable.ic_barcode, null, tint = c.inkSecondary, size = 34.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.xs)) {
            Text(info?.name ?: stringResource(R.string.product_unknown), style = InfTheme.type.titleMedium.copy(lineHeight = InfTheme.type.titleMedium.fontSize * 1.15f), color = c.ink)
            Text(stringResource(R.string.product_code, stringResource(item.format.label()), parsed.code), style = InfTheme.type.monoSmall, color = c.inkSecondary)
        }
    }
    val pricesAvailable = product.prices != PricesState.Disabled
    if (pricesAvailable) {
        Column {
        Row(Modifier.fillMaxWidth()) {
            ProductTab.entries.forEach { tab ->
                val on = product.tab == tab
                Column(
                    Modifier.weight(1f).height(48.dp).selectable(selected = on, role = Role.Tab, onClick = { onEvent(ResultEvent.SelectTab(tab)) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(stringResource(if (tab == ProductTab.DETAILS) R.string.product_tab_details else R.string.product_tab_prices), style = InfTheme.type.tab, color = if (on) c.primary else c.inkSecondary)
                    }
                    Box(Modifier.fillMaxWidth().height(3.dp).background(if (on) c.primary else Color.Transparent))
                }
            }
        }
        HorizontalDivider(color = c.outline)
        }
    }
    if (!pricesAvailable || product.tab == ProductTab.DETAILS) DetailsTab(product.details, parsed.code) else PricesTab(product.prices, onEvent)
}

@Composable
private fun DetailsTab(details: DetailsState, code: String) {
    val c = InfTheme.colors
    when (details) {
        DetailsState.Loading -> Box(Modifier.fillMaxWidth().height(96.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
        is DetailsState.Loaded -> Column {
            if (details.info.brand.isNotBlank()) DetailRow(stringResource(R.string.field_brand), details.info.brand)
            if (details.info.quantity.isNotBlank()) DetailRow(stringResource(R.string.field_quantity), details.info.quantity)
            DetailRow(stringResource(R.string.field_barcode), code, mono = true)
        }
        is DetailsState.Failed -> Text(
            stringResource(if (details.reason == FailureReason.OFFLINE) R.string.product_details_offline else R.string.product_details_missing),
            style = InfTheme.type.bodyMedium,
            color = c.inkSecondary,
        )
    }
}

@Composable
private fun PricesTab(prices: PricesState, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    when (prices) {
        PricesState.Disabled -> Unit
        PricesState.Loading -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = c.primary) }
        is PricesState.Loaded -> PricesList(prices.offers, onEvent)
        is PricesState.Failed -> Column(
            Modifier.fillMaxWidth().padding(vertical = Space.xl, horizontal = Space.md),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            val offline = prices.reason == FailureReason.OFFLINE
            Box(Modifier.size(80.dp).clip(Shapes.pill).background(c.surfaceVariant), contentAlignment = Alignment.Center) {
                InfIcon(if (offline) R.drawable.ic_wifi_off else R.drawable.ic_warning, null, tint = c.inkSecondary, size = 38.dp)
            }
            Text(stringResource(if (offline) R.string.prices_offline_title else R.string.prices_error_title), style = InfTheme.type.titleMedium, color = c.ink, textAlign = TextAlign.Center)
            Text(stringResource(if (offline) R.string.prices_offline_body else R.string.prices_error_body), style = InfTheme.type.bodyMedium.copy(lineHeight = InfTheme.type.bodyMedium.fontSize * 1.5f), color = c.inkSecondary, textAlign = TextAlign.Center)
            Column(Modifier.fillMaxWidth().padding(top = Space.xs), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                InfButton(stringResource(R.string.action_retry), { onEvent(ResultEvent.RetryPrices) }, icon = R.drawable.ic_history)
                InfButton(stringResource(R.string.prices_back_to_details), { onEvent(ResultEvent.SelectTab(ProductTab.DETAILS)) }, kind = ButtonKind.Tonal)
            }
        }
    }
}

@Composable
private fun PricesList(offers: List<PriceOffer>, onEvent: (ResultEvent) -> Unit) {
    val c = InfTheme.colors
    val best = offers.firstOrNull { it.isBest } ?: return
    Row(
        Modifier.fillMaxWidth().clip(Shapes.cardLarge).background(c.primary).padding(Space.lg),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
            Text(stringResource(R.string.prices_lowest), style = InfTheme.type.actionLabel, color = c.onPrimary)
            Text(formatPrice(best), style = InfTheme.type.price, color = c.onPrimary)
            Text("${best.store} · ${best.detail}", style = InfTheme.type.chipLabel, color = c.onPrimary)
        }
        best.url?.let { url ->
            Surface(onClick = { onEvent(ResultEvent.OpenOffer(url)) }, shape = Shapes.pill, color = c.onPrimary, modifier = Modifier.height(44.dp)) {
                Row(Modifier.padding(horizontal = Space.lg), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfIcon(R.drawable.ic_open_in_new, null, tint = c.primary, size = 18.dp)
                    Text(stringResource(R.string.action_open), style = InfTheme.type.tab, color = c.primary)
                }
            }
        }
    }
    Column {
        offers.forEach { offer ->
            HorizontalDivider(color = c.outline)
            Row(Modifier.fillMaxWidth().heightIn(min = 60.dp).padding(vertical = Space.sm), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.md)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Space.xxs)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Space.sm)) {
                        Text(offer.store, style = InfTheme.type.rowTitle, color = c.ink)
                        if (offer.isBest) {
                            Text(
                                stringResource(R.string.prices_best),
                                style = InfTheme.type.caption.copy(fontWeight = FontWeight.ExtraBold),
                                color = c.onAccent,
                                modifier = Modifier.clip(Shapes.pill).background(c.accent).padding(horizontal = Space.sm, vertical = Space.xxs),
                            )
                        }
                    }
                    Text(offer.detail, style = InfTheme.type.bodySmall, color = c.inkSecondary)
                }
                Text(formatPrice(offer), style = InfTheme.type.mono.copy(fontSize = InfTheme.type.bodyLarge.fontSize), color = c.ink)
            }
        }
    }
    Text(stringResource(R.string.prices_footnote), style = InfTheme.type.caption.copy(fontWeight = FontWeight.Normal), color = c.inkSecondary)
}

private fun formatPrice(offer: PriceOffer): String =
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(offer.currency) }.format(offer.priceCents / 100.0)
