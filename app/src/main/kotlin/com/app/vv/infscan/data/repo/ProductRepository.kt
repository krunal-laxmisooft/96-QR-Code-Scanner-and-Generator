package com.app.vv.infscan.data.repo

import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.core.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

data class ProductInfo(
    val code: String,
    val name: String,
    val brand: String,
    val quantity: String,
)

interface ProductRepository {
    suspend fun lookup(code: String): AppResult<ProductInfo>
}

@Singleton
class OpenFoodFactsProductRepository @Inject constructor(
    private val network: NetworkMonitor,
    @IoDispatcher private val io: CoroutineDispatcher,
) : ProductRepository {

    override suspend fun lookup(code: String): AppResult<ProductInfo> {
        if (!network.isOnline()) return AppResult.Failure(FailureReason.OFFLINE)
        return withContext(io) {
            runCatching {
                val url = URL("$BASE/api/v2/product/$code.json?fields=product_name,brands,quantity")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    setRequestProperty("User-Agent", USER_AGENT)
                }
                try {
                    if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                        return@runCatching AppResult.Failure(FailureReason.NOT_FOUND)
                    }
                    parse(code, conn.inputStream.bufferedReader().use { it.readText() })
                } finally {
                    conn.disconnect()
                }
            }.getOrElse { AppResult.Failure(FailureReason.SERVICE) }
        }
    }

    internal fun parse(code: String, body: String): AppResult<ProductInfo> {
        val root: JsonObject = Json.parseToJsonElement(body).jsonObject
        if (root["status"]?.jsonPrimitive?.int != 1) return AppResult.Failure(FailureReason.NOT_FOUND)
        val product = root["product"]?.jsonObject ?: return AppResult.Failure(FailureReason.NOT_FOUND)
        val name = product["product_name"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        if (name.isEmpty()) return AppResult.Failure(FailureReason.NOT_FOUND)
        return AppResult.Success(
            ProductInfo(
                code = code,
                name = name,
                brand = product["brands"]?.jsonPrimitive?.contentOrNull.orEmpty().substringBefore(',').trim(),
                quantity = product["quantity"]?.jsonPrimitive?.contentOrNull.orEmpty().trim(),
            ),
        )
    }

    private companion object {
        const val BASE = "https://world.openfoodfacts.org"
        const val TIMEOUT_MS = 8_000
        const val USER_AGENT = "InfScan/1.0 (Android)"
    }
}
