package com.app.vv.infscan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.app.vv.infscan.core.AppResult
import com.app.vv.infscan.core.FailureReason
import com.app.vv.infscan.data.db.InfDatabase
import com.app.vv.infscan.data.prefs.AppSettings
import com.app.vv.infscan.data.prefs.DataStoreSettingsRepository
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.data.repo.FakePriceRepository
import com.app.vv.infscan.data.repo.NetworkMonitor
import com.app.vv.infscan.data.repo.OpenFoodFactsProductRepository
import com.app.vv.infscan.data.repo.PriceCheckFlag
import com.app.vv.infscan.data.repo.RoomHistoryRepository
import com.app.vv.infscan.domain.model.CodeFormat
import com.app.vv.infscan.domain.model.CodeKind
import com.app.vv.infscan.domain.model.HistoryKind
import com.app.vv.infscan.domain.model.NewHistoryItem
import com.app.vv.infscan.domain.model.ScanSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class RoomHistoryRepositoryTest {
    private lateinit var db: InfDatabase
    private lateinit var repo: RoomHistoryRepository

    @Before fun open() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(), InfDatabase::class.java).allowMainThreadQueries().build()
        repo = RoomHistoryRepository(db.historyDao())
    }

    @After fun close() = db.close()

    private fun item(raw: String, at: Long, kind: HistoryKind = HistoryKind.SCANNED) =
        NewHistoryItem(kind, CodeKind.TEXT, raw, raw, CodeFormat.QR, ScanSource.CAMERA, at)

    @Test fun `items are returned newest first per kind`() = runTest {
        repo.add(item("old", 1))
        repo.add(item("new", 3))
        repo.add(item("created", 2, HistoryKind.CREATED))
        assertEquals(listOf("new", "old"), repo.observe(HistoryKind.SCANNED).first().map { it.raw })
        assertEquals(listOf("created"), repo.observe(HistoryKind.CREATED).first().map { it.raw })
        assertEquals(2, repo.observeCount(HistoryKind.SCANNED).first())
        assertEquals(listOf("new", "created", "old"), repo.all().map { it.raw })
        assertEquals(listOf("new"), repo.latest(HistoryKind.SCANNED, 1).map { it.raw })
    }

    @Test fun `favorite delete and restore keep the same id`() = runTest {
        val id = repo.add(item("one", 1))
        repo.setFavorite(id, true)
        val saved = repo.get(id)!!
        assertTrue(saved.favorite)
        repo.delete(id)
        assertNull(repo.get(id))
        repo.restore(saved)
        assertEquals(saved, repo.get(id))
        assertEquals(saved, repo.observeItem(id).first())
    }

    @Test fun `purge removes old items but keeps favorites and handles forever`() = runTest {
        val day = TimeUnit.DAYS.toMillis(1)
        val now = 100 * day
        val oldId = repo.add(item("old", now - 40 * day))
        val favId = repo.add(item("old favorite", now - 41 * day))
        repo.setFavorite(favId, true)
        repo.add(item("recent", now - 2 * day))
        assertEquals(0, repo.purgeOlderThan(0, now))
        assertEquals(1, repo.purgeOlderThan(30, now))
        assertNull(repo.get(oldId))
        assertEquals(listOf("recent", "old favorite"), repo.all().map { it.raw })
    }

    @Test fun `clear removes everything`() = runTest {
        repo.add(item("a", 1))
        repo.add(item("b", 2, HistoryKind.CREATED))
        repo.clear()
        assertTrue(repo.all().isEmpty())
    }
}

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {
    private val repo = DataStoreSettingsRepository(ApplicationProvider.getApplicationContext())

    @Test fun `defaults and updates`() = runTest {
        repo.setThemeMode(ThemeMode.SYSTEM)
        repo.setVibrateOnScan(true)
        repo.setOpenLinksWithoutAsking(false)
        repo.setRetentionDays(AppSettings.DEFAULT_RETENTION_DAYS)
        val defaults = repo.current()
        assertEquals(ThemeMode.SYSTEM, defaults.themeMode)
        assertTrue(defaults.vibrateOnScan)
        assertFalse(defaults.openLinksWithoutAsking)
        assertEquals(30, defaults.retentionDays)

        repo.setThemeMode(ThemeMode.DARK)
        repo.setVibrateOnScan(false)
        repo.setOpenLinksWithoutAsking(true)
        repo.setRetentionDays(7)
        repo.setOnboardingDone()
        val changed = repo.settings.first()
        assertEquals(ThemeMode.DARK, changed.themeMode)
        assertFalse(changed.vibrateOnScan)
        assertTrue(changed.openLinksWithoutAsking)
        assertEquals(7, changed.retentionDays)
        assertTrue(changed.onboardingDone)
    }

    @Test fun `first launch time is stored once and ad time round trips`() = runTest {
        val first = repo.firstLaunchAt()
        assertEquals(first, repo.firstLaunchAt())
        repo.setLastAppOpenAdAt(1234L)
        assertEquals(1234L, repo.lastAppOpenAdAt())
    }
}

class ProductAndPriceTest {
    private class Network(var online: Boolean) : NetworkMonitor {
        override fun isOnline() = online
    }

    private val network = Network(true)
    private val products = OpenFoodFactsProductRepository(network, Dispatchers.Unconfined)

    @Test fun `open food facts response is parsed`() {
        val body = """{"status":1,"product":{"product_name":" Oat Barista Drink ","brands":"Oatly, Other","quantity":"1 L"}}"""
        val info = (products.parse("5012345678900", body) as AppResult.Success).value
        assertEquals("Oat Barista Drink", info.name)
        assertEquals("Oatly", info.brand)
        assertEquals("1 L", info.quantity)
    }

    @Test fun `unknown products and empty names are not found`() {
        assertEquals(AppResult.Failure(FailureReason.NOT_FOUND), products.parse("1", """{"status":0}"""))
        assertEquals(AppResult.Failure(FailureReason.NOT_FOUND), products.parse("1", """{"status":1,"product":{"product_name":" "}}"""))
        assertEquals(AppResult.Failure(FailureReason.NOT_FOUND), products.parse("1", """{"status":1}"""))
    }

    @Test fun `offline lookups fail without a request`() = runTest {
        network.online = false
        assertEquals(AppResult.Failure(FailureReason.OFFLINE), products.lookup("5012345678900"))
    }

    @Test fun `stand-in prices are stable, sorted around one best price and respect offline`() = runTest {
        val prices = FakePriceRepository(network, PriceCheckFlag(true))
        val first = (prices.offers("5012345678900") as AppResult.Success).value
        val again = (prices.offers("5012345678900") as AppResult.Success).value
        assertEquals(first, again)
        assertEquals(1, first.count { it.isBest })
        assertEquals(first.minOf { it.priceCents }, first.first { it.isBest }.priceCents)
        assertTrue(prices.isEnabled)
        network.online = false
        assertEquals(AppResult.Failure(FailureReason.OFFLINE), prices.offers("5012345678900"))
        assertFalse(FakePriceRepository(network, PriceCheckFlag(false)).isEnabled)
    }
}
