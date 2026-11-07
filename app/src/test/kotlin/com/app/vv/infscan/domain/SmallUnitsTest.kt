package com.app.vv.infscan.domain

import com.app.vv.infscan.ads.AppOpenPolicy
import com.app.vv.infscan.ads.InterstitialPolicy
import com.app.vv.infscan.data.repo.CsvExporter
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryItem
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.ScanSource
import com.app.vv.infscan.domain.parser.LinkSafetyChecker
import com.app.vv.infscan.platform.BarcodeFormats
import com.app.vv.infscan.ui.common.DayBucket
import com.app.vv.infscan.ui.common.RelativeTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class LinkSafetyCheckerTest {
    @Test fun `https links are secure and others are flagged`() {
        assertTrue(LinkSafetyChecker.check("https://cafelumen.example/menu").isHttps)
        assertFalse(LinkSafetyChecker.check("http://cafelumen.example").isHttps)
    }

    @Test fun `known shorteners are detected without www`() {
        assertTrue(LinkSafetyChecker.check("https://bit.ly/3abc").isShortened)
        assertTrue(LinkSafetyChecker.check("https://www.tinyurl.com/x").isShortened)
        assertFalse(LinkSafetyChecker.check("https://cafelumen.example").isShortened)
    }

    @Test fun `host strips www and survives garbage`() {
        assertEquals("example.com", LinkSafetyChecker.displayHost("https://www.example.com/a?b=1"))
        assertEquals("", LinkSafetyChecker.displayHost("::::"))
    }
}

class CsvExporterTest {
    private fun item(title: String, raw: String) =
        HistoryItem(1, HistoryKind.SCANNED, CodeKind.TEXT, raw, title, CodeFormat.QR, ScanSource.CAMERA, 0L, true)

    @Test fun `header and a plain row`() {
        val lines = CsvExporter.toCsv(listOf(item("Table 12", "Table 12"))).lines()
        assertEquals("id,kind,type,format,source,created_at,favorite,title,content", lines[0])
        assertEquals("1,scanned,text,qr,camera,1970-01-01T00:00:00Z,true,Table 12,Table 12", lines[1])
    }

    @Test fun `commas quotes and new lines are escaped`() {
        val csv = CsvExporter.toCsv(listOf(item("a,b", "say \"hi\"\nnow")))
        assertTrue(csv.contains("\"a,b\""))
        assertTrue(csv.contains("\"say \"\"hi\"\"\nnow\""))
    }

    @Test fun `spreadsheet formulas are neutralised`() {
        val csv = CsvExporter.toCsv(listOf(item("=SUM(A1)", "+1")))
        assertTrue(csv.contains(",'=SUM(A1),'+1"))
    }
}

class AdPolicyTest {
    @Test fun `interstitial waits three minutes and stops after three`() {
        val policy = InterstitialPolicy()
        val min = TimeUnit.MINUTES.toMillis(1)
        assertTrue(policy.canShow(0))
        policy.onShown(0)
        assertFalse(policy.canShow(2 * min))
        assertTrue(policy.canShow(3 * min))
        policy.onShown(3 * min)
        policy.onShown(7 * min)
        assertFalse(policy.canShow(60 * min))
    }

    @Test fun `app open skips first launch and respects four hours`() {
        val hour = TimeUnit.HOURS.toMillis(1)
        assertFalse(AppOpenPolicy.canShow(10 * hour, 0, launchedBefore = false, lastShownAt = 0))
        assertTrue(AppOpenPolicy.canShow(10 * hour, 0, launchedBefore = true, lastShownAt = 0))
        assertFalse(AppOpenPolicy.canShow(10 * hour, 0, launchedBefore = true, lastShownAt = 8 * hour))
        assertTrue(AppOpenPolicy.canShow(13 * hour, 0, launchedBefore = true, lastShownAt = 8 * hour))
    }
}

class BarcodeFormatsTest {
    @Test fun `ml kit constants map to formats`() {
        assertEquals(CodeFormat.QR, BarcodeFormats.fromMlKit(256))
        assertEquals(CodeFormat.EAN_13, BarcodeFormats.fromMlKit(32))
        assertEquals(CodeFormat.UPC_A, BarcodeFormats.fromMlKit(512))
        assertEquals(CodeFormat.UNKNOWN, BarcodeFormats.fromMlKit(999999))
        assertTrue(CodeFormat.EAN_8.isProduct)
        assertFalse(CodeFormat.QR.isProduct)
    }
}

class RelativeTimeTest {
    private val zone = ZoneOffset.UTC
    private fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 11, day, hour, 0).toInstant(ZoneOffset.UTC).toEpochMilli()

    @Test fun `buckets follow calendar days`() {
        val now = at(7, 12)
        assertEquals(DayBucket.TODAY, RelativeTime.bucket(at(7, 1), now, zone))
        assertEquals(DayBucket.YESTERDAY, RelativeTime.bucket(at(6, 23), now, zone))
        assertEquals(DayBucket.THIS_WEEK, RelativeTime.bucket(at(3, 8), now, zone))
        assertEquals(DayBucket.THIS_WEEK, RelativeTime.bucket(at(1, 8), now, zone))
        assertEquals(DayBucket.OLDER, RelativeTime.bucket(LocalDateTime.of(2026, 10, 20, 8, 0).toInstant(ZoneOffset.UTC).toEpochMilli(), now, zone))
    }
}
