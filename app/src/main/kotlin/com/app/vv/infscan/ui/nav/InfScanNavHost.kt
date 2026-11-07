package com.app.vv.infscan.ui.nav

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.app.vv.infscan.domain.create.CreateType
import com.app.vv.infscan.ui.components.InfBottomNav
import com.app.vv.infscan.ui.components.MainTab
import com.app.vv.infscan.ui.create.CreateFormRoute
import com.app.vv.infscan.ui.create.CreateHomeRoute
import com.app.vv.infscan.ui.create.CreatePreviewRoute
import com.app.vv.infscan.ui.history.HistoryRoute
import com.app.vv.infscan.ui.onboarding.OnboardingRoute
import com.app.vv.infscan.ui.result.ResultRoute
import com.app.vv.infscan.ui.scan.ScanRoute
import com.app.vv.infscan.ui.settings.SettingsRoute
import com.app.vv.infscan.ui.theme.InfTheme

@Composable
fun InfScanNavHost(onboardingDone: Boolean, darkTheme: Boolean) {
    val navController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val destination = backStack?.destination
    val tab = when {
        destination?.hasRoute<ScanDest>() == true -> MainTab.SCAN
        destination?.hasRoute<CreateDest>() == true -> MainTab.CREATE
        destination?.hasRoute<HistoryDest>() == true -> MainTab.HISTORY
        destination?.hasRoute<SettingsDest>() == true -> MainTab.SETTINGS
        else -> null
    }
    Column(Modifier.fillMaxSize().background(InfTheme.colors.background)) {
        NavHost(
            navController = navController,
            startDestination = if (onboardingDone) ScanDest else OnboardingDest,
            modifier = Modifier.weight(1f),
        ) {
            composable<OnboardingDest> {
                OnboardingRoute(onFinished = {
                    navController.navigate(ScanDest) { popUpTo<OnboardingDest> { inclusive = true } }
                })
            }
            composable<ScanDest> {
                ScanRoute(
                    onOpenResult = { id -> navController.navigate(ResultDest(id)) },
                    onOpenHistory = { navController.navigateToTab(MainTab.HISTORY) },
                )
            }
            composable<CreateDest> {
                CreateHomeRoute(
                    onSelect = { type: CreateType -> navController.navigate(CreateFormDest(type.name)) },
                    onOpenRecent = { id -> navController.navigate(CreatePreviewDest(id)) },
                )
            }
            composable<HistoryDest> {
                HistoryRoute(
                    onOpen = { id -> navController.navigate(ResultDest(id)) },
                    onScan = { navController.navigateToTab(MainTab.SCAN) },
                )
            }
            composable<SettingsDest> { SettingsRoute() }
            composable<ResultDest> { ResultRoute(onBack = { navController.popBackStack() }) }
            composable<CreateFormDest> {
                CreateFormRoute(
                    onBack = { navController.popBackStack() },
                    onPreview = { id -> navController.navigate(CreatePreviewDest(id)) { popUpTo<CreateDest>() } },
                )
            }
            composable<CreatePreviewDest> { CreatePreviewRoute(onBack = { navController.popBackStack() }) }
        }
        if (tab != null) InfBottomNav(selected = tab, onSelect = { navController.navigateToTab(it) })
    }
}

private fun NavHostController.navigateToTab(tab: MainTab) {
    val route: Any = when (tab) {
        MainTab.SCAN -> ScanDest
        MainTab.CREATE -> CreateDest
        MainTab.HISTORY -> HistoryDest
        MainTab.SETTINGS -> SettingsDest
    }
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
