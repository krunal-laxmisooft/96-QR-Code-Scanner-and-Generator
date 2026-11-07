package com.app.vv.infscan.ui.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Space {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val screen = 20.dp
    val fieldGap = 22.dp
    val xl = 24.dp
    val xxl = 32.dp
}

object Radius {
    val input = 12.dp
    val card = 16.dp
    val cardLarge = 20.dp
    val hero = 24.dp
    val sheet = 28.dp
}

object Shapes {
    val input = RoundedCornerShape(Radius.input)
    val card = RoundedCornerShape(Radius.card)
    val cardLarge = RoundedCornerShape(Radius.cardLarge)
    val hero = RoundedCornerShape(Radius.hero)
    val sheet = RoundedCornerShape(topStart = Radius.sheet, topEnd = Radius.sheet)
    val dialog = RoundedCornerShape(Radius.sheet)
    val pill = CircleShape
}

object Sizes {
    val touch = 48.dp
    val topBar = 56.dp
    val bottomNav = 72.dp
    val button = 52.dp
    val iconSmall = 18.dp
    val icon = 24.dp
    val avatar = 40.dp
    val chip = 36.dp
    val field = 56.dp
    val bannerWidth = 320.dp
    val bannerHeight = 50.dp
}
