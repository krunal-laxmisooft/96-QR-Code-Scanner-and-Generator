package com.app.vv.infscan.ui.onboarding

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.vv.infscan.R
import com.app.vv.infscan.analytics.AnalyticsEvents
import com.app.vv.infscan.analytics.AnalyticsTracker
import com.app.vv.infscan.data.prefs.SettingsRepository
import com.app.vv.infscan.ui.components.ButtonKind
import com.app.vv.infscan.ui.components.InfButton
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Space
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val PAGE_COUNT = 3

private data class OnboardingPage(@StringRes val title: Int, @StringRes val body: Int)

private val pages = listOf(
    OnboardingPage(R.string.onboarding_title_1, R.string.onboarding_body_1),
    OnboardingPage(R.string.onboarding_title_2, R.string.onboarding_body_2),
    OnboardingPage(R.string.onboarding_title_3, R.string.onboarding_body_3),
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val settings: SettingsRepository,
    private val analytics: AnalyticsTracker,
) : ViewModel() {
    fun finish(onDone: () -> Unit) {
        viewModelScope.launch {
            settings.setOnboardingDone()
            analytics.log(AnalyticsEvents.ONBOARDING_COMPLETE)
            onDone()
        }
    }
}

@Composable
fun OnboardingRoute(onFinished: () -> Unit, viewModel: OnboardingViewModel = hiltViewModel()) {
    val pager = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    OnboardingScreen(
        pager = pager,
        onNext = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } },
        onFinish = { viewModel.finish(onFinished) },
    )
}

@Composable
fun OnboardingScreen(pager: PagerState, onNext: () -> Unit, onFinish: () -> Unit) {
    val c = InfTheme.colors
    val last = pager.currentPage == PAGE_COUNT - 1
    Column(Modifier.fillMaxSize().background(c.background)) {
        Row(
            Modifier.statusBarsPadding().fillMaxWidth().height(56.dp).padding(start = Space.screen, end = Space.xs),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(painterResource(R.drawable.ic_logo_mark), null, Modifier.width(32.dp).height(32.dp))
            if (!last) InfButton(stringResource(R.string.onboarding_skip), onFinish, kind = ButtonKind.Text, height = 48.dp, fillWidth = false)
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { index -> PageContent(index) }
        Column(
            Modifier.navigationBarsPadding().padding(start = Space.screen, end = Space.screen, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.lg),
        ) {
            Dots(pager.currentPage)
            if (last) {
                Text(
                    stringResource(R.string.onboarding_footnote),
                    style = InfTheme.type.bodySmall,
                    color = c.inkSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Space.sm),
                )
            }
            InfButton(stringResource(if (last) R.string.onboarding_start else R.string.onboarding_next), if (last) onFinish else onNext)
        }
    }
}

@Composable
private fun PageContent(index: Int) {
    val c = InfTheme.colors
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
    ) {
        when (index) {
            0 -> ScanIllustration()
            1 -> CreateIllustration()
            else -> PriceIllustration()
        }
        Column(verticalArrangement = Arrangement.spacedBy(Space.md), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(pages[index].title), style = InfTheme.type.titleLarge, color = c.ink, textAlign = TextAlign.Center)
            Text(stringResource(pages[index].body), style = InfTheme.type.bodyLarge.copy(lineHeight = InfTheme.type.bodyLarge.fontSize * 1.5f), color = c.inkSecondary, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun Dots(current: Int) {
    val c = InfTheme.colors
    Row(Modifier.fillMaxWidth().height(16.dp), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
        repeat(PAGE_COUNT) { i ->
            Box(
                Modifier
                    .height(8.dp)
                    .width(if (i == current) 24.dp else 8.dp)
                    .clip(Shapes.pill)
                    .background(if (i == current) c.primary else c.outline),
            )
        }
    }
}
