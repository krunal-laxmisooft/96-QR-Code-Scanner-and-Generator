package com.app.vv.infscan.data.repo

import com.app.vv.infscan.core.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

interface LinkResolver {
    /** Follows redirects of a shortened link without opening it in a browser. Null when it can't be resolved. */
    suspend fun resolve(url: String): String?
}

@Singleton
class HttpLinkResolver @Inject constructor(
    private val network: NetworkMonitor,
    @IoDispatcher private val io: CoroutineDispatcher,
) : LinkResolver {

    override suspend fun resolve(url: String): String? {
        if (!network.isOnline()) return null
        return withContext(io) {
            var current = url
            repeat(MAX_HOPS) {
                val next = runCatching { redirectTarget(current) }.getOrNull() ?: return@withContext current.takeIf { it != url }
                current = next
            }
            current.takeIf { it != url }
        }
    }

    private fun redirectTarget(url: String): String? {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            requestMethod = "HEAD"
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
        }
        return try {
            if (conn.responseCode in 300..399) conn.getHeaderField("Location")?.let { URL(URL(url), it).toString() } else null
        } finally {
            conn.disconnect()
        }
    }

    private companion object {
        const val MAX_HOPS = 5
        const val TIMEOUT_MS = 5_000
    }
}
