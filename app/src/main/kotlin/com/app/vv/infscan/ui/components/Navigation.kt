package com.app.vv.infscan.ui.components

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.app.vv.infscan.R
import com.app.vv.infscan.ui.theme.InfTheme
import com.app.vv.infscan.ui.theme.Shapes
import com.app.vv.infscan.ui.theme.Sizes
import com.app.vv.infscan.ui.theme.Space
import androidx.compose.foundation.shape.RoundedCornerShape

enum class MainTab(@StringRes val label: Int, @DrawableRes val icon: Int) {
    SCAN(R.string.tab_scan, R.drawable.ic_scan),
    CREATE(R.string.tab_create, R.drawable.ic_qr),
    HISTORY(R.string.tab_history, R.drawable.ic_history),
    SETTINGS(R.string.tab_settings, R.drawable.ic_settings),
}

@Composable
fun InfBottomNav(selected: MainTab, onSelect: (MainTab) -> Unit, modifier: Modifier = Modifier) {
    val c = InfTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(c.surface)
            .windowInsetsPadding(WindowInsets.navigationBars),
    ) {
        HorizontalDivider(color = c.outline, thickness = 1.dp)
        Row(Modifier.height(Sizes.bottomNav)) {
            MainTab.entries.forEach { tab ->
                val on = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                        .selectable(selected = on, role = Role.Tab, onClick = { onSelect(tab) }),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Space.xs, Alignment.CenterVertically),
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 56.dp, height = 28.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (on) c.soft else androidx.compose.ui.graphics.Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) {
                        InfIcon(tab.icon, null, tint = if (on) c.onSoft else c.inkSecondary)
                    }
                    Text(
                        text = stringResource(tab.label),
                        style = InfTheme.type.navLabel.let { if (on) it.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold) else it },
                        color = if (on) c.ink else c.inkSecondary,
                    )
                }
            }
        }
    }
}
