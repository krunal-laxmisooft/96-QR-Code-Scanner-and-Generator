package com.app.vv.infscan.data.repo

import android.net.Uri
import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.core.FailureReason
import javax.inject.Inject
import javax.inject.Singleton

data class PriceOffer(
    val store: String,
    val detail: String,
    val priceCents: Int,
    val currency: String,
    val isBest: Boolean,
    val url: String? = null,
)

interface PriceRepository {
    val isEnabled: Boolean
    suspend fun offers(code: String): AppResult<List<PriceOffer>>
}

/**
 * Stand-in until a real price provider is chosen. It answers from the barcode digits so the
 * Prices tab and its states can be exercised. Replace by binding another PriceRepository in DI.
 */
@Singleton
class FakePriceRepository @Inject constructor(
    private val network: NetworkMonitor,
    private val enabled: PriceCheckFlag,
) : PriceRepository {

    override val isEnabled: Boolean get() = enabled.value

    override suspend fun offers(code: String): AppResult<List<PriceOffer>> {
        if (!network.isOnline()) return AppResult.Failure(FailureReason.OFFLINE)
        val seed = code.fold(7) { acc, c -> (acc * 31 + c.code) and 0x7fffffff }
        val base = 199 + seed % 300
        val offers = listOf(
            Triple("FreshMart", "In store · 1.2 km", base),
            Triple("CornerGrocer", "In store · 0.6 km", base + 20 + seed % 15),
            Triple("MegaStore", "In store · 3.4 km", base + 36 + seed % 25),
            Triple("QuickCart", "Online · plus 3.99 shipping", base + 50 + seed % 30),
        )
        val best = offers.minOf { it.third }
        return AppResult.Success(
            offers.map { (store, detail, cents) ->
                PriceOffer(store, detail, cents, "USD", cents == best, url = "https://www.google.com/search?q=${Uri.encode("$store $code")}")
            },
        )
    }
}

data class PriceCheckFlag(val value: Boolean)
