package com.app.vv.infscan.domain.model

sealed interface ParsedCode {
    val kind: CodeKind

    data class Url(val url: String) : ParsedCode {
        override val kind = CodeKind.URL
    }

    data class Wifi(
        val ssid: String,
        val security: WifiSecurity,
        val password: String,
        val hidden: Boolean,
    ) : ParsedCode {
        override val kind = CodeKind.WIFI
    }

    data class Contact(
        val name: String,
        val phone: String,
        val email: String,
        val organization: String,
    ) : ParsedCode {
        override val kind = CodeKind.CONTACT
    }

    data class Event(
        val title: String,
        val startMillis: Long?,
        val endMillis: Long?,
        val location: String,
        val description: String,
    ) : ParsedCode {
        override val kind = CodeKind.EVENT
    }

    data class Sms(val number: String, val body: String) : ParsedCode {
        override val kind = CodeKind.SMS
    }

    data class Email(val address: String, val subject: String, val body: String) : ParsedCode {
        override val kind = CodeKind.EMAIL
    }

    data class Phone(val number: String) : ParsedCode {
        override val kind = CodeKind.PHONE
    }

    data class Location(val latitude: Double, val longitude: Double, val label: String) : ParsedCode {
        override val kind = CodeKind.LOCATION
    }

    data class Product(val code: String) : ParsedCode {
        override val kind = CodeKind.PRODUCT
    }

    data class Text(val text: String) : ParsedCode {
        override val kind = CodeKind.TEXT
    }
}

enum class WifiSecurity(val payloadValue: String) {
    WPA("WPA"),
    WEP("WEP"),
    NONE("nopass");

    companion object {
        fun fromPayload(value: String?): WifiSecurity = when (value?.uppercase()) {
            "WEP" -> WEP
            "NOPASS", "" , null -> NONE
            else -> WPA
        }
    }
}
