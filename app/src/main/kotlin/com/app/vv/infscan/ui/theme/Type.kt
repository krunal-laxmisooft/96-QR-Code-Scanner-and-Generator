package com.app.vv.infscan.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.app.vv.infscan.R

@OptIn(ExperimentalTextApi::class)
private fun variable(resId: Int, vararg weights: Int): FontFamily = FontFamily(
    weights.map { w ->
        Font(
            resId = resId,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(FontVariation.weight(w)),
        )
    },
)

val DisplayFamily = variable(R.font.bricolage_grotesque, 500, 700, 800)
val BodyFamily = variable(R.font.figtree, 400, 500, 600, 700, 800)
val MonoFamily = variable(R.font.jetbrains_mono, 500)

private fun style(
    family: FontFamily,
    size: Int,
    weight: Int,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
) = TextStyle(
    fontFamily = family,
    fontSize = size.sp,
    fontWeight = FontWeight(weight),
    lineHeight = lineHeight,
    letterSpacing = letterSpacing,
)

@Immutable
data class InfTypography(
    val display: TextStyle = style(DisplayFamily, 44, 800, 44.sp, (-0.02).em),
    val titleLarge: TextStyle = style(DisplayFamily, 30, 800, 33.sp, (-0.02).em),
    val titleMedium: TextStyle = style(DisplayFamily, 22, 700, 26.sp, (-0.01).em),
    val titleSmall: TextStyle = style(DisplayFamily, 18, 700, 22.sp),
    val deniedTitle: TextStyle = style(DisplayFamily, 26, 700, 30.sp, (-0.01).em),
    val result: TextStyle = style(DisplayFamily, 28, 700, 31.sp, (-0.01).em),
    val price: TextStyle = style(DisplayFamily, 38, 800, 40.sp, (-0.02).em),
    val bodyLarge: TextStyle = style(BodyFamily, 16, 400, 24.sp),
    val bodyMedium: TextStyle = style(BodyFamily, 14, 400, 20.sp),
    val bodySmall: TextStyle = style(BodyFamily, 13, 400, 18.sp),
    val rowTitle: TextStyle = style(BodyFamily, 16, 600, 22.sp),
    val button: TextStyle = style(BodyFamily, 16, 700, 22.sp),
    val tab: TextStyle = style(BodyFamily, 15, 700, 20.sp),
    val navLabel: TextStyle = style(BodyFamily, 12, 600, 16.sp),
    val label: TextStyle = style(BodyFamily, 12, 800, 16.sp, 0.08.em),
    val caption: TextStyle = style(BodyFamily, 12, 600, 16.sp),
    val actionLabel: TextStyle = style(BodyFamily, 13, 700, 16.sp),
    val chipLabel: TextStyle = style(BodyFamily, 14, 600, 20.sp),
    val typeChip: TextStyle = style(BodyFamily, 14, 700, 20.sp),
    val adLabel: TextStyle = style(BodyFamily, 10, 800, 12.sp, 0.04.em),
    val mono: TextStyle = style(MonoFamily, 14, 500, 20.sp),
    val monoSmall: TextStyle = style(MonoFamily, 13, 500, 18.sp),
)

fun InfTypography.toMaterial(): Typography = Typography(
    displayLarge = display,
    headlineLarge = titleLarge,
    headlineMedium = titleMedium,
    headlineSmall = titleSmall,
    titleLarge = titleMedium,
    titleMedium = rowTitle,
    titleSmall = tab,
    bodyLarge = bodyLarge,
    bodyMedium = bodyMedium,
    bodySmall = bodySmall,
    labelLarge = button,
    labelMedium = caption,
    labelSmall = navLabel,
)
