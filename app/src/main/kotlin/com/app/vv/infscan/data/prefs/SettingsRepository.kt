package com.app.vv.infscan.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val vibrateOnScan: Boolean = true,
    val openLinksWithoutAsking: Boolean = false,
    val retentionDays: Int = DEFAULT_RETENTION_DAYS,
    val onboardingDone: Boolean = false,
) {
    companion object {
        const val DEFAULT_RETENTION_DAYS = 30
        const val KEEP_FOREVER = 0
        val RETENTION_OPTIONS = listOf(7, 30, 90, KEEP_FOREVER)
    }
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setVibrateOnScan(value: Boolean)
    suspend fun setOpenLinksWithoutAsking(value: Boolean)
    suspend fun setRetentionDays(days: Int)
    suspend fun setOnboardingDone()
    suspend fun firstLaunchAt(): Long
    suspend fun lastAppOpenAdAt(): Long
    suspend fun setLastAppOpenAdAt(millis: Long)
}

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "infscan_settings")

@Singleton
class DataStoreSettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private val store get() = context.dataStore

    override val settings: Flow<AppSettings> = store.data.map { p ->
        AppSettings(
            themeMode = p[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
            vibrateOnScan = p[VIBRATE] ?: true,
            openLinksWithoutAsking = p[OPEN_LINKS] ?: false,
            retentionDays = p[RETENTION] ?: AppSettings.DEFAULT_RETENTION_DAYS,
            onboardingDone = p[ONBOARDING] ?: false,
        )
    }

    override suspend fun current(): AppSettings = settings.first()

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[THEME] = mode.name }
    override suspend fun setVibrateOnScan(value: Boolean) = edit { it[VIBRATE] = value }
    override suspend fun setOpenLinksWithoutAsking(value: Boolean) = edit { it[OPEN_LINKS] = value }
    override suspend fun setRetentionDays(days: Int) = edit { it[RETENTION] = days }
    override suspend fun setOnboardingDone() = edit { it[ONBOARDING] = true }

    override suspend fun firstLaunchAt(): Long {
        val existing = store.data.first()[FIRST_LAUNCH]
        if (existing != null) return existing
        val now = System.currentTimeMillis()
        edit { it[FIRST_LAUNCH] = now }
        return now
    }

    override suspend fun lastAppOpenAdAt(): Long = store.data.first()[LAST_APP_OPEN] ?: 0L
    override suspend fun setLastAppOpenAdAt(millis: Long) = edit { it[LAST_APP_OPEN] = millis }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit { block(it) }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val VIBRATE = booleanPreferencesKey("vibrate_on_scan")
        val OPEN_LINKS = booleanPreferencesKey("open_links_without_asking")
        val RETENTION = intPreferencesKey("retention_days")
        val ONBOARDING = booleanPreferencesKey("onboarding_done")
        val FIRST_LAUNCH = longPreferencesKey("first_launch_at")
        val LAST_APP_OPEN = longPreferencesKey("last_app_open_ad_at")
    }
}
