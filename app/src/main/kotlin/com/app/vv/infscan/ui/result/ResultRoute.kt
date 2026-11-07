package com.app.vv.infscan.ui.result

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import android.provider.CalendarContract
import android.provider.ContactsContract
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.vv.infscan.R
import com.app.vv.infscan.ads.AdPlacement
import com.app.vv.infscan.ads.NativeAdSlot
import com.app.vv.infscan.platform.Sharing
import com.app.vv.infscan.ui.components.InfSnackbarHost

@Composable
fun ResultRoute(onBack: () -> Unit, viewModel: ResultViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is ResultEffect.Launch -> if (!startAction(context, effect.action)) snackbar.showSnackbar(resources.getString(R.string.result_no_app))
                is ResultEffect.ShareText -> Sharing.shareText(context, effect.text, resources.getString(R.string.share_chooser))
                is ResultEffect.CopyText -> {
                    Sharing.copy(context, resources.getString(R.string.app_name), effect.text)
                    snackbar.showSnackbar(resources.getString(effect.message))
                }
                ResultEffect.Close -> onBack()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        ResultScreen(state, viewModel::onEvent, onBack, adSlot = { NativeAdSlot(AdPlacement.NATIVE_RESULT) })
        InfSnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp))
    }
}

private fun startAction(context: Context, action: ResultAction): Boolean {
    val intent = when (action) {
        is ResultAction.OpenUrl -> Intent(Intent.ACTION_VIEW, action.url.toUri())
        is ResultAction.Dial -> Intent(Intent.ACTION_DIAL, "tel:${Uri.encode(action.number)}".toUri())
        is ResultAction.Sms -> Intent(Intent.ACTION_SENDTO, "smsto:${Uri.encode(action.number)}".toUri()).putExtra("sms_body", action.body)
        is ResultAction.Email -> Intent(Intent.ACTION_SENDTO, "mailto:".toUri()).apply {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(action.address))
            putExtra(Intent.EXTRA_SUBJECT, action.subject)
            putExtra(Intent.EXTRA_TEXT, action.body)
        }
        is ResultAction.Map -> Intent(Intent.ACTION_VIEW, "geo:${action.latitude},${action.longitude}?q=${action.latitude},${action.longitude}(${Uri.encode(action.label)})".toUri())
        is ResultAction.AddContact -> Intent(ContactsContract.Intents.Insert.ACTION).apply {
            type = ContactsContract.RawContacts.CONTENT_TYPE
            putExtra(ContactsContract.Intents.Insert.NAME, action.contact.name)
            putExtra(ContactsContract.Intents.Insert.PHONE, action.contact.phone)
            putExtra(ContactsContract.Intents.Insert.EMAIL, action.contact.email)
            putExtra(ContactsContract.Intents.Insert.COMPANY, action.contact.organization)
        }
        is ResultAction.AddEvent -> Intent(Intent.ACTION_INSERT, CalendarContract.Events.CONTENT_URI).apply {
            putExtra(CalendarContract.Events.TITLE, action.event.title)
            putExtra(CalendarContract.Events.EVENT_LOCATION, action.event.location)
            putExtra(CalendarContract.Events.DESCRIPTION, action.event.description)
            action.event.startMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, it) }
            action.event.endMillis?.let { putExtra(CalendarContract.EXTRA_EVENT_END_TIME, it) }
        }
    }
    return try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
