package com.app.vv.infscan.domain.parser

import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.WifiSecurity
import java.net.URLDecoder
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

object CodeParser {

    fun parse(raw: String, format: CodeFormat = CodeFormat.QR): ParsedCode {
        val text = raw.trim()
        val upper = text.uppercase()
        return when {
            format.isProduct && text.all(Char::isDigit) -> ParsedCode.Product(text)
            upper.startsWith("WIFI:") -> parseWifi(text) ?: ParsedCode.Text(text)
            upper.startsWith("BEGIN:VCARD") -> parseVCard(text)
            upper.startsWith("MECARD:") -> parseMeCard(text)
            upper.startsWith("BEGIN:VEVENT") || upper.startsWith("BEGIN:VCALENDAR") -> parseEvent(text)
            upper.startsWith("GEO:") -> parseGeo(text) ?: ParsedCode.Text(text)
            upper.startsWith("TEL:") -> ParsedCode.Phone(text.substring(4).trim())
            upper.startsWith("SMSTO:") || upper.startsWith("SMS:") -> parseSms(text)
            upper.startsWith("MAILTO:") -> parseMailto(text)
            upper.startsWith("MATMSG:") -> parseMatmsg(text)
            upper.startsWith("HTTP://") || upper.startsWith("HTTPS://") -> ParsedCode.Url(text)
            upper.startsWith("WWW.") && !text.contains(' ') -> ParsedCode.Url("https://$text")
            else -> ParsedCode.Text(text)
        }
    }

    fun title(code: ParsedCode): String = when (code) {
        is ParsedCode.Url -> LinkSafetyChecker.displayHost(code.url)
        is ParsedCode.Wifi -> code.ssid
        is ParsedCode.Contact -> code.name.ifBlank { code.phone.ifBlank { code.email } }
        is ParsedCode.Event -> code.title
        is ParsedCode.Sms -> code.number
        is ParsedCode.Email -> code.address
        is ParsedCode.Phone -> code.number
        is ParsedCode.Location -> code.label.ifBlank { "%.5f, %.5f".format(code.latitude, code.longitude) }
        is ParsedCode.Product -> code.code
        is ParsedCode.Text -> code.text.lineSequence().first().take(80)
    }

    private fun parseWifi(text: String): ParsedCode.Wifi? {
        val fields = splitEscaped(text.substring(5), ';')
        val map = fields.mapNotNull { f ->
            val idx = indexOfUnescaped(f, ':')
            if (idx <= 0) null else f.substring(0, idx).uppercase() to unescape(f.substring(idx + 1))
        }.toMap()
        val ssid = map["S"] ?: return null
        return ParsedCode.Wifi(
            ssid = ssid,
            security = WifiSecurity.fromPayload(map["T"]),
            password = map["P"].orEmpty(),
            hidden = map["H"].equals("true", ignoreCase = true),
        )
    }

    private fun parseVCard(text: String): ParsedCode.Contact {
        val lines = unfold(text)
        fun value(vararg keys: String): String = lines.firstNotNullOfOrNull { line ->
            val key = line.substringBefore(':').substringBefore(';').uppercase()
            if (key in keys) line.substringAfter(':', "").trim() else null
        }.orEmpty()
        val structured = value("N").split(';').filter { it.isNotBlank() }.let { parts ->
            if (parts.isEmpty()) "" else (parts.drop(1) + parts.first()).joinToString(" ")
        }
        return ParsedCode.Contact(
            name = value("FN").ifBlank { structured },
            phone = value("TEL"),
            email = value("EMAIL"),
            organization = value("ORG").replace(';', ' ').trim(),
        )
    }

    private fun parseMeCard(text: String): ParsedCode.Contact {
        val map = splitEscaped(text.substring(7), ';').mapNotNull { f ->
            val idx = f.indexOf(':')
            if (idx <= 0) null else f.substring(0, idx).uppercase() to unescape(f.substring(idx + 1))
        }.toMap()
        val name = map["N"].orEmpty().split(',').reversed().joinToString(" ").trim()
        return ParsedCode.Contact(name, map["TEL"].orEmpty(), map["EMAIL"].orEmpty(), map["ORG"].orEmpty())
    }

    private fun parseEvent(text: String): ParsedCode.Event {
        val lines = unfold(text)
        fun value(key: String): String = lines.firstNotNullOfOrNull { line ->
            if (line.substringBefore(':').substringBefore(';').uppercase() == key) line.substringAfter(':', "").trim() else null
        }.orEmpty()
        return ParsedCode.Event(
            title = value("SUMMARY"),
            startMillis = parseIcsDate(value("DTSTART")),
            endMillis = parseIcsDate(value("DTEND")),
            location = value("LOCATION"),
            description = value("DESCRIPTION"),
        )
    }

    internal fun parseIcsDate(value: String): Long? {
        if (value.isBlank()) return null
        return runCatching {
            when {
                value.endsWith("Z") -> LocalDateTime.parse(value.dropLast(1), ICS_DATE_TIME).toInstant(ZoneOffset.UTC).toEpochMilli()
                value.contains('T') -> LocalDateTime.parse(value, ICS_DATE_TIME).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                else -> LocalDate.parse(value, ICS_DATE).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        }.getOrNull()
    }

    private fun parseGeo(text: String): ParsedCode.Location? {
        val body = text.substring(4).substringBefore('?')
        val parts = body.split(',')
        val lat = parts.getOrNull(0)?.toDoubleOrNull() ?: return null
        val lon = parts.getOrNull(1)?.toDoubleOrNull() ?: return null
        val label = queryParams(text.substringAfter('?', "")).let { it["q"].orEmpty() }
            .substringAfter('(', "").substringBefore(')')
        return ParsedCode.Location(lat, lon, label)
    }

    private fun parseSms(text: String): ParsedCode.Sms {
        val body = text.substringAfter(':')
        return if (text.uppercase().startsWith("SMSTO:")) {
            ParsedCode.Sms(body.substringBefore(':'), body.substringAfter(':', ""))
        } else {
            ParsedCode.Sms(body.substringBefore('?'), queryParams(body.substringAfter('?', ""))["body"].orEmpty())
        }
    }

    private fun parseMailto(text: String): ParsedCode.Email {
        val body = text.substring(7)
        val params = queryParams(body.substringAfter('?', ""))
        return ParsedCode.Email(decode(body.substringBefore('?')), params["subject"].orEmpty(), params["body"].orEmpty())
    }

    private fun parseMatmsg(text: String): ParsedCode.Email {
        val map = splitEscaped(text.substring(7), ';').mapNotNull { f ->
            val idx = f.indexOf(':')
            if (idx <= 0) null else f.substring(0, idx).uppercase() to unescape(f.substring(idx + 1))
        }.toMap()
        return ParsedCode.Email(map["TO"].orEmpty(), map["SUB"].orEmpty(), map["BODY"].orEmpty())
    }

    private fun queryParams(query: String): Map<String, String> =
        query.split('&').filter { it.contains('=') }.associate {
            decode(it.substringBefore('=')).lowercase() to decode(it.substringAfter('='))
        }

    private fun decode(value: String): String = runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    private fun unfold(text: String): List<String> {
        val out = mutableListOf<String>()
        text.lines().forEach { line ->
            if ((line.startsWith(" ") || line.startsWith("\t")) && out.isNotEmpty()) {
                out[out.lastIndex] = out.last() + line.trimStart()
            } else {
                out += line.trimEnd('\r')
            }
        }
        return out
    }

    private fun splitEscaped(text: String, delimiter: Char): List<String> {
        val parts = mutableListOf<String>()
        val current = StringBuilder()
        var escaped = false
        for (c in text) {
            when {
                escaped -> { current.append('\\').append(c); escaped = false }
                c == '\\' -> escaped = true
                c == delimiter -> { parts += current.toString(); current.clear() }
                else -> current.append(c)
            }
        }
        if (current.isNotEmpty()) parts += current.toString()
        return parts.filter { it.isNotEmpty() }
    }

    private fun indexOfUnescaped(text: String, target: Char): Int {
        var escaped = false
        text.forEachIndexed { i, c ->
            if (escaped) escaped = false
            else if (c == '\\') escaped = true
            else if (c == target) return i
        }
        return -1
    }

    private fun unescape(value: String): String = buildString {
        var escaped = false
        for (c in value) {
            if (escaped) { append(c); escaped = false } else if (c == '\\') escaped = true else append(c)
        }
    }

    private val ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
    private val ICS_DATE = DateTimeFormatter.ofPattern("yyyyMMdd")
}
