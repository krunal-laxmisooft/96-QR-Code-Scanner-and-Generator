package com.app.vv.infscan.domain.parser

import java.net.URI

data class LinkSafety(
    val host: String,
    val isHttps: Boolean,
    val isShortened: Boolean,
)

object LinkSafetyChecker {
    private val shorteners = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "ow.ly", "is.gd", "buff.ly", "cutt.ly", "rb.gy",
        "rebrand.ly", "shorturl.at", "tiny.cc", "s.id", "lnkd.in", "t.ly",
    )

    fun check(url: String): LinkSafety {
        val host = displayHost(url)
        return LinkSafety(
            host = host,
            isHttps = url.startsWith("https://", ignoreCase = true),
            isShortened = host in shorteners,
        )
    }

    fun displayHost(url: String): String =
        runCatching { URI(url.trim()).host }.getOrNull().orEmpty().removePrefix("www.")
}
