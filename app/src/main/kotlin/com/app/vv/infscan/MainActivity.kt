package com.app.vv.infscan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.app.vv.infscan.ads.AdManager
import com.app.vv.infscan.ads.LocalAdManager
import com.app.vv.infscan.ads.LocalAnalytics
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.prefs.ThemeMode
import com.app.vv.infscan.ui.nav.InfScanNavHost
import com.app.vv.infscan.ui.theme.InfScanTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var adManager: AdManager
    @Inject lateinit var analytics: AnalyticsTracker

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        splash.setKeepOnScreenCondition { !viewModel.uiState.value.ready }
        setContent {
            val state by viewModel.uiState.collectAsStateWithLifecycle()
            val dark = when (state.settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            InfScanTheme(darkTheme = dark) {
                CompositionLocalProvider(LocalAdManager provides adManager, LocalAnalytics provides analytics) {
                    if (state.ready) {
                        InfScanNavHost(onboardingDone = state.settings.onboardingDone, darkTheme = dark)
                    }
                    LaunchedEffect(state.ready, state.settings.onboardingDone, state.adsAllowedThisSession) {
                        if (state.ready && state.settings.onboardingDone && state.adsAllowedThisSession) {
                            lifecycleScope.launch {
                                adManager.start(this@MainActivity)
                                adManager.showAppOpenIfAllowed(this@MainActivity, launchedBefore = true)
                            }
                        }
                    }
                }
            }
        }
    }
}
