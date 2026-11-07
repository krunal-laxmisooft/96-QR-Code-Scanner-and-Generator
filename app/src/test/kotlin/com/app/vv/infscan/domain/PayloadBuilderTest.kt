package com.app.vv.infscan.domain

import com.app.vv.infscan.R
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.domain.create.FormField
import com.app.vv.infscan.domain.create.FormValues
import com.app.vv.infscan.domain.create.PayloadBuilder
import com.app.vv.infscan.domain.create.PayloadResult
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.domain.parser.CodeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadBuilderTest {

    private fun valid(type: CreateType, values: FormValues): String =
        (PayloadBuilder.build(type, values) as PayloadResult.Valid).payload

    private fun errors(type: CreateType, values: FormValues): Map<FormField, Int> =
        (PayloadBuilder.build(type, values) as PayloadResult.Invalid).errors

    private fun text(vararg pairs: Pair<FormField, String>) = FormValues(text = mapOf(*pairs))

    @Test fun `website adds https and validates`() {
        assertEquals("https://cafelumen.example", valid(CreateType.WEBSITE, text(FormField.URL to "cafelumen.example")))
        assertEquals("http://a.example/x", valid(CreateType.WEBSITE, text(FormField.URL to "http://a.example/x")))
        assertEquals(R.string.error_required, errors(CreateType.WEBSITE, text())[FormField.URL])
        assertEquals(R.string.error_url, errors(CreateType.WEBSITE, text(FormField.URL to "not a url"))[FormField.URL])
        assertEquals(R.string.error_url, errors(CreateType.WEBSITE, text(FormField.URL to "localhost"))[FormField.URL])
    }

    @Test fun `text is trimmed and required`() {
        assertEquals("Table 12", valid(CreateType.TEXT, text(FormField.TEXT to "  Table 12 ")))
        assertEquals(R.string.error_required, errors(CreateType.TEXT, text(FormField.TEXT to "   "))[FormField.TEXT])
    }

    @Test fun `wifi round trips with special characters`() {
        val payload = valid(CreateType.WIFI, text(FormField.SSID to "Lumen;Guest", FormField.PASSWORD to "pa:ss,\\word").copy(hidden = true))
        assertEquals("WIFI:T:WPA;S:Lumen\\;Guest;P:pa\\:ss\\,\\\\word;H:true;;", payload)
        val parsed = CodeParser.parse(payload) as ParsedCode.Wifi
        assertEquals("Lumen;Guest", parsed.ssid)
        assertEquals("pa:ss,\\word", parsed.password)
        assertTrue(parsed.hidden)
    }

    @Test fun `open wifi needs no password`() {
        val payload = valid(CreateType.WIFI, text(FormField.SSID to "Cafe").copy(security = WifiSecurity.NONE))
        assertEquals("WIFI:T:nopass;S:Cafe;;", payload)
        assertEquals(R.string.error_required, errors(CreateType.WIFI, text(FormField.SSID to "Cafe"))[FormField.PASSWORD])
    }

    @Test fun `contact needs a name and a way to reach them`() {
        val e = errors(CreateType.CONTACT, text(FormField.CONTACT_NAME to "Alex Moreno"))
        assertEquals(R.string.error_phone_or_email, e[FormField.PHONE])
        assertEquals(R.string.error_email, errors(CreateType.CONTACT, text(FormField.CONTACT_NAME to "A", FormField.EMAIL to "nope"))[FormField.EMAIL])
        val payload = valid(CreateType.CONTACT, text(FormField.CONTACT_NAME to "Alex Moreno", FormField.PHONE to "+1 555 0100", FormField.ORGANIZATION to "Lumen"))
        val parsed = CodeParser.parse(payload) as ParsedCode.Contact
        assertEquals("Alex Moreno", parsed.name)
        assertEquals("+1 555 0100", parsed.phone)
        assertEquals("Lumen", parsed.organization)
    }

    @Test fun `message and email round trip`() {
        assertEquals(ParsedCode.Sms("5550100", "Hi there"), CodeParser.parse(valid(CreateType.MESSAGE, text(FormField.SMS_NUMBER to "5550100", FormField.MESSAGE to "Hi there"))))
        val email = valid(CreateType.EMAIL, text(FormField.TO to "a@b.example", FormField.SUBJECT to "Hello & welcome", FormField.BODY to "Line one"))
        assertEquals(ParsedCode.Email("a@b.example", "Hello & welcome", "Line one"), CodeParser.parse(email))
        assertEquals(R.string.error_email, errors(CreateType.EMAIL, text(FormField.TO to "a@b"))[FormField.TO])
    }

    @Test fun `event keeps utc times and checks order`() {
        val start = 1_775_988_000_000L
        val payload = valid(CreateType.EVENT, text(FormField.EVENT_TITLE to "Spring Market", FormField.EVENT_LOCATION to "Old Square").copy(startMillis = start, endMillis = start + 3_600_000L))
        val parsed = CodeParser.parse(payload) as ParsedCode.Event
        assertEquals(start, parsed.startMillis)
        assertEquals(start + 3_600_000L, parsed.endMillis)
        assertEquals("Old Square", parsed.location)
        val bad = errors(CreateType.EVENT, text(FormField.EVENT_TITLE to "X").copy(startMillis = start, endMillis = start - 1))
        assertEquals(R.string.error_event_end, bad[FormField.EVENT_LOCATION])
    }

    @Test fun `location validates ranges and round trips`() {
        val payload = valid(CreateType.LOCATION, text(FormField.LATITUDE to "51.5007", FormField.LONGITUDE to "-0.1246", FormField.PLACE_NAME to "Big Ben"))
        val parsed = CodeParser.parse(payload) as ParsedCode.Location
        assertEquals("Big Ben", parsed.label)
        assertEquals(51.5007, parsed.latitude, 0.0)
        val e = errors(CreateType.LOCATION, text(FormField.LATITUDE to "95", FormField.LONGITUDE to "x"))
        assertEquals(R.string.error_latitude, e[FormField.LATITUDE])
        assertEquals(R.string.error_longitude, e[FormField.LONGITUDE])
    }

    @Test fun `every type lists at least one field`() {
        CreateType.entries.forEach { assertTrue(it.name, PayloadBuilder.fields(it).isNotEmpty()) }
    }
}
