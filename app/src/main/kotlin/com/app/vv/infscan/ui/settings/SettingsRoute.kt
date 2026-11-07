package com.app.vv.infscan.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.vv.infscan.BuildConfig
import com.app.vv.infscan.R
import com.app.vv.infscan.ads.LocalAdManager
import com.app.vv.infscan.ads.findActivity
import com.app.vv.infscan.platform.Sharing
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.launch

@Composable
fun SettingsRoute(viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val ads = LocalAdManager.current
    val scope = rememberCoroutineScope()
    val state by viewModel.state.collectAsStateWithLifecycle()
    SettingsScreen(
        state = state,
        onEvent = viewModel::onEvent,
        onAdChoices = { context.findActivity()?.let { ads?.showPrivacyOptions(it) } },
        onPrivacyPolicy = { Sharing.openUrl(context, BuildConfig.PRIVACY_POLICY_URL) },
        onRate = {
            val activity = context.findActivity() ?: return@SettingsScreen
            scope.launch {
                runCatching {
                    val manager = ReviewManagerFactory.create(context)
                    manager.launchReview(activity, manager.requestReview())
                }.onFailure { Sharing.openUrl(context, "https://play.google.com/store/apps/details?id=${context.packageName}") }
            }
        },
        onFeedback = {
            Sharing.sendFeedback(context, BuildConfig.FEEDBACK_EMAIL, context.getString(R.string.settings_feedback_subject, BuildConfig.VERSION_NAME))
        },
    )
}
