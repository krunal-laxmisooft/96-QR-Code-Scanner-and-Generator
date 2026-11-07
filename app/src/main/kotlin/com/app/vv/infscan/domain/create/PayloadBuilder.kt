package com.app.vv.infscan.domain.create

import androidx.annotation.StringRes
import com.app.vv.infscan.R
import com.app.vv.infscan.domain.model.WifiSecurity
import java.net.URLEncoder
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

enum class FormField(@StringRes val label: Int) {
    URL(R.string.field_website),
    TEXT(R.string.field_text),
    SSID(R.string.field_network_name),
    PASSWORD(R.string.field_password),
    CONTACT_NAME(R.string.field_name),
    PHONE(R.string.field_phone),
    EMAIL(R.string.field_email),
    ORGANIZATION(R.string.field_organization),
    SMS_NUMBER(R.string.field_phone),
    MESSAGE(R.string.field_message),
    TO(R.string.field_to),
    SUBJECT(R.string.field_subject),
    BODY(R.string.field_body),
    EVENT_TITLE(R.string.field_event_title),
    EVENT_LOCATION(R.string.field_event_location),
    LATITUDE(R.string.field_latitude),
    LONGITUDE(R.string.field_longitude),
    PLACE_NAME(R.string.field_place_name),
}

data class FormValues(
    val text: Map<FormField, String> = emptyMap(),
    val security: WifiSecurity = WifiSecurity.WPA,
    val hidden: Boolean = false,
    val startMillis: Long? = null,
    val endMillis: Long? = null,
) {
    fun value(field: FormField): String = text[field].orEmpty().trim()
}

sealed interface PayloadResult {
    data class Valid(val payload: String) : PayloadResult
    data class Invalid(val errors: Map<FormField, Int>) : PayloadResult
}

object PayloadBuilder {

    fun fields(type: CreateType): List<FormField> = when (type) {
        CreateType.WEBSITE -> listOf(FormField.URL)
        CreateType.TEXT -> listOf(FormField.TEXT)
        CreateType.WIFI -> listOf(FormField.SSID, FormField.PASSWORD)
        CreateType.CONTACT -> listOf(FormField.CONTACT_NAME, FormField.PHONE, FormField.EMAIL, FormField.ORGANIZATION)
        CreateType.MESSAGE -> listOf(FormField.SMS_NUMBER, FormField.MESSAGE)
        CreateType.EMAIL -> listOf(FormField.TO, FormField.SUBJECT, FormField.BODY)
        CreateType.EVENT -> listOf(FormField.EVENT_TITLE, FormField.EVENT_LOCATION)
        CreateType.LOCATION -> listOf(FormField.LATITUDE, FormField.LONGITUDE, FormField.PLACE_NAME)
    }

    fun build(type: CreateType, values: FormValues): PayloadResult {
        val errors = validate(type, values)
        if (errors.isNotEmpty()) return PayloadResult.Invalid(errors)
        return PayloadResult.Valid(
            when (type) {
                CreateType.WEBSITE -> normalizeUrl(values.value(FormField.URL))
                CreateType.TEXT -> values.value(FormField.TEXT)
                CreateType.WIFI -> wifi(values)
                CreateType.CONTACT -> contact(values)
                CreateType.MESSAGE -> "SMSTO:${values.value(FormField.SMS_NUMBER)}:${values.value(FormField.MESSAGE)}"
                CreateType.EMAIL -> email(values)
                CreateType.EVENT -> event(values)
                CreateType.LOCATION -> location(values)
            },
        )
    }

    fun validate(type: CreateType, values: FormValues): Map<FormField, Int> {
        val errors = mutableMapOf<FormField, Int>()
        fun require(field: FormField, @StringRes message: Int = R.string.error_required) {
            if (values.value(field).isEmpty()) errors[field] = message
        }
        when (type) {
            CreateType.WEBSITE -> {
                require(FormField.URL)
                if (FormField.URL !in errors && !looksLikeUrl(values.value(FormField.URL))) errors[FormField.URL] = R.string.error_url
            }
            CreateType.TEXT -> require(FormField.TEXT)
            CreateType.WIFI -> {
                require(FormField.SSID)
                if (values.security != WifiSecurity.NONE) require(FormField.PASSWORD)
            }
            CreateType.CONTACT -> {
                require(FormField.CONTACT_NAME)
                val email = values.value(FormField.EMAIL)
                if (email.isNotEmpty() && !email.matches(EMAIL_REGEX)) errors[FormField.EMAIL] = R.string.error_email
                if (values.value(FormField.PHONE).isEmpty() && email.isEmpty()) errors[FormField.PHONE] = R.string.error_phone_or_email
            }
            CreateType.MESSAGE -> require(FormField.SMS_NUMBER)
            CreateType.EMAIL -> {
                require(FormField.TO)
                if (FormField.TO !in errors && !values.value(FormField.TO).matches(EMAIL_REGEX)) errors[FormField.TO] = R.string.error_email
            }
            CreateType.EVENT -> {
                require(FormField.EVENT_TITLE)
                val start = values.startMillis
                val end = values.endMillis
                if (start != null && end != null && end < start) errors[FormField.EVENT_LOCATION] = R.string.error_event_end
            }
            CreateType.LOCATION -> {
                val lat = values.value(FormField.LATITUDE).toDoubleOrNull()
                val lon = values.value(FormField.LONGITUDE).toDoubleOrNull()
                if (lat == null || lat !in -90.0..90.0) errors[FormField.LATITUDE] = R.string.error_latitude
                if (lon == null || lon !in -180.0..180.0) errors[FormField.LONGITUDE] = R.string.error_longitude
            }
        }
        return errors
    }

    fun normalizeUrl(input: String): String =
        if (input.contains("://")) input else "https://$input"

    private fun looksLikeUrl(input: String): Boolean {
        val host = input.substringAfter("://").substringBefore('/').substringBefore('?')
        return !input.contains(' ') && host.contains('.') && !host.startsWith('.') && !host.endsWith('.')
    }

    private fun wifi(values: FormValues): String {
        val security = values.security
        val password = if (security == WifiSecurity.NONE) "" else escape(values.value(FormField.PASSWORD))
        return buildString {
            append("WIFI:T:").append(security.payloadValue)
            append(";S:").append(escape(values.value(FormField.SSID)))
            if (password.isNotEmpty()) append(";P:").append(password)
            if (values.hidden) append(";H:true")
            append(";;")
        }
    }

    private fun contact(values: FormValues): String = buildString {
        val name = values.value(FormField.CONTACT_NAME)
        appendLine("BEGIN:VCARD")
        appendLine("VERSION:3.0")
        appendLine("N:${nameParts(name)}")
        appendLine("FN:$name")
        values.value(FormField.ORGANIZATION).takeIf { it.isNotEmpty() }?.let { appendLine("ORG:$it") }
        values.value(FormField.PHONE).takeIf { it.isNotEmpty() }?.let { appendLine("TEL:$it") }
        values.value(FormField.EMAIL).takeIf { it.isNotEmpty() }?.let { appendLine("EMAIL:$it") }
        append("END:VCARD")
    }

    private fun nameParts(name: String): String {
        val parts = name.split(' ').filter { it.isNotEmpty() }
        return if (parts.size <= 1) name else "${parts.last()};${parts.dropLast(1).joinToString(" ")}"
    }

    private fun email(values: FormValues): String {
        val query = listOfNotNull(
            values.value(FormField.SUBJECT).takeIf { it.isNotEmpty() }?.let { "subject=${encode(it)}" },
            values.value(FormField.BODY).takeIf { it.isNotEmpty() }?.let { "body=${encode(it)}" },
        ).joinToString("&")
        return "mailto:${values.value(FormField.TO)}" + if (query.isEmpty()) "" else "?$query"
    }

    private fun event(values: FormValues): String = buildString {
        appendLine("BEGIN:VCALENDAR")
        appendLine("VERSION:2.0")
        appendLine("BEGIN:VEVENT")
        appendLine("SUMMARY:${values.value(FormField.EVENT_TITLE)}")
        values.startMillis?.let { appendLine("DTSTART:${icsUtc(it)}") }
        values.endMillis?.let { appendLine("DTEND:${icsUtc(it)}") }
        values.value(FormField.EVENT_LOCATION).takeIf { it.isNotEmpty() }?.let { appendLine("LOCATION:$it") }
        appendLine("END:VEVENT")
        append("END:VCALENDAR")
    }

    private fun location(values: FormValues): String {
        val lat = values.value(FormField.LATITUDE)
        val lon = values.value(FormField.LONGITUDE)
        val name = values.value(FormField.PLACE_NAME)
        return if (name.isEmpty()) "geo:$lat,$lon" else "geo:$lat,$lon?q=$lat,$lon(${encode(name)})"
    }

    private fun icsUtc(millis: Long): String = ICS.format(Instant.ofEpochMilli(millis).atOffset(ZoneOffset.UTC))

    private fun escape(value: String): String = buildString {
        value.forEach { c ->
            if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') append('\\')
            append(c)
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8").replace("+", "%20")

    private val EMAIL_REGEX = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    private val ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
}
