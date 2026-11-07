package com.app.vv.infscan.domain

import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.ParsedCode
import com.app.vv.infscan.domain.model.WifiSecurity
import com.app.vv.infscan.domain.parser.CodeParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CodeParserTest {

    @Test fun `https and http links are urls`() {
        assertEquals(ParsedCode.Url("https://cafelumen.example/menu"), CodeParser.parse("https://cafelumen.example/menu"))
        assertEquals(ParsedCode.Url("HTTP://Example.com"), CodeParser.parse("HTTP://Example.com"))
    }

    @Test fun `www addresses get https`() {
        assertEquals(ParsedCode.Url("https://www.example.com"), CodeParser.parse("www.example.com"))
    }

    @Test fun `plain words with spaces stay text`() {
        assertEquals(ParsedCode.Text("www. is not a link"), CodeParser.parse("www. is not a link"))
        assertEquals(ParsedCode.Text("Table 12"), CodeParser.parse("Table 12"))
    }

    @Test fun `numeric product formats become products`() {
        assertEquals(ParsedCode.Product("5012345678900"), CodeParser.parse("5012345678900", CodeFormat.EAN_13))
        assertEquals(ParsedCode.Text("5012345678900"), CodeParser.parse("5012345678900", CodeFormat.QR))
        assertEquals(ParsedCode.Text("ABC123"), CodeParser.parse("ABC123", CodeFormat.UPC_A))
    }

    @Test fun `wifi payload with escapes`() {
        val parsed = CodeParser.parse("WIFI:T:WPA;S:My\\;Net;P:pa\\:ss\\\\word;H:true;;") as ParsedCode.Wifi
        assertEquals("My;Net", parsed.ssid)
        assertEquals("pa:ss\\word", parsed.password)
        assertEquals(WifiSecurity.WPA, parsed.security)
        assertTrue(parsed.hidden)
    }

    @Test fun `wifi open network and missing ssid`() {
        val open = CodeParser.parse("WIFI:T:nopass;S:Cafe;;") as ParsedCode.Wifi
        assertEquals(WifiSecurity.NONE, open.security)
        assertEquals("", open.password)
        assertEquals(ParsedCode.Text("WIFI:T:WPA;P:x;;"), CodeParser.parse("WIFI:T:WPA;P:x;;"))
    }

    @Test fun `vcard fields`() {
        val card = "BEGIN:VCARD\nVERSION:3.0\nN:Moreno;Alex\nFN:Alex Moreno\nORG:Lumen\nTEL;TYPE=CELL:+1 555 0100\nEMAIL:alex@lumen.example\nEND:VCARD"
        val parsed = CodeParser.parse(card) as ParsedCode.Contact
        assertEquals("Alex Moreno", parsed.name)
        assertEquals("+1 555 0100", parsed.phone)
        assertEquals("alex@lumen.example", parsed.email)
        assertEquals("Lumen", parsed.organization)
    }

    @Test fun `vcard falls back to structured name`() {
        val parsed = CodeParser.parse("BEGIN:VCARD\nN:Moreno;Alex\nEND:VCARD") as ParsedCode.Contact
        assertEquals("Alex Moreno", parsed.name)
    }

    @Test fun `mecard contact`() {
        val parsed = CodeParser.parse("MECARD:N:Moreno,Alex;TEL:5550100;EMAIL:a@b.example;;") as ParsedCode.Contact
        assertEquals("Alex Moreno", parsed.name)
        assertEquals("5550100", parsed.phone)
    }

    @Test fun `calendar event with utc times`() {
        val ics = "BEGIN:VCALENDAR\nBEGIN:VEVENT\nSUMMARY:Spring Market\nDTSTART:20260412T100000Z\nDTEND:20260412T120000Z\nLOCATION:Old Square\nEND:VEVENT\nEND:VCALENDAR"
        val parsed = CodeParser.parse(ics) as ParsedCode.Event
        assertEquals("Spring Market", parsed.title)
        assertEquals("Old Square", parsed.location)
        assertEquals(7_200_000L, parsed.endMillis!! - parsed.startMillis!!)
    }

    @Test fun `bad event dates are ignored`() {
        val parsed = CodeParser.parse("BEGIN:VEVENT\nSUMMARY:X\nDTSTART:soon\nEND:VEVENT") as ParsedCode.Event
        assertEquals(null, parsed.startMillis)
    }

    @Test fun `geo uri with label`() {
        val parsed = CodeParser.parse("geo:51.5007,-0.1246?q=51.5007,-0.1246(Big%20Ben)") as ParsedCode.Location
        assertEquals(51.5007, parsed.latitude, 0.0)
        assertEquals(-0.1246, parsed.longitude, 0.0)
        assertEquals("Big Ben", parsed.label)
    }

    @Test fun `geo without numbers stays text`() {
        assertTrue(CodeParser.parse("geo:abc") is ParsedCode.Text)
    }

    @Test fun `tel sms mailto and matmsg`() {
        assertEquals(ParsedCode.Phone("+15550100"), CodeParser.parse("tel:+15550100"))
        assertEquals(ParsedCode.Sms("5550100", "Hi there"), CodeParser.parse("SMSTO:5550100:Hi there"))
        assertEquals(ParsedCode.Sms("5550100", "Hi there"), CodeParser.parse("sms:5550100?body=Hi%20there"))
        assertEquals(ParsedCode.Email("a@b.example", "Hello", "Body text"), CodeParser.parse("mailto:a@b.example?subject=Hello&body=Body%20text"))
        assertEquals(ParsedCode.Email("a@b.example", "Hello", "Body"), CodeParser.parse("MATMSG:TO:a@b.example;SUB:Hello;BODY:Body;;"))
    }

    @Test fun `titles are short and readable`() {
        assertEquals("cafelumen.example", CodeParser.title(CodeParser.parse("https://www.cafelumen.example/menu")))
        assertEquals("Lumen-Guest", CodeParser.title(CodeParser.parse("WIFI:T:WPA;S:Lumen-Guest;P:x;;")))
        assertEquals("first line", CodeParser.title(CodeParser.parse("first line\nsecond")))
        assertEquals(80, CodeParser.title(CodeParser.parse("x".repeat(200))).length)
        assertNotNull(CodeParser.title(CodeParser.parse("geo:1,2")))
    }
}
