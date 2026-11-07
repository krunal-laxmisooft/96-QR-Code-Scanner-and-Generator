package com.app.vv.infscan.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.app.vv.infscan.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

enum class DayBucket { TODAY, YESTERDAY, THIS_WEEK, OLDER }

object RelativeTime {
    fun bucket(then: Long, now: Long, zone: ZoneId): DayBucket {
        val day = Instant.ofEpochMilli(then).atZone(zone).toLocalDate()
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val diff = java.time.temporal.ChronoUnit.DAYS.between(day, today)
        return when {
            diff <= 0 -> DayBucket.TODAY
            diff == 1L -> DayBucket.YESTERDAY
            diff < 7 -> DayBucket.THIS_WEEK
            else -> DayBucket.OLDER
        }
    }

    fun time(then: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale).format(Instant.ofEpochMilli(then).atZone(zone))

    fun weekday(then: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofPattern("EEE", locale).format(Instant.ofEpochMilli(then).atZone(zone))

    fun date(then: Long, zone: ZoneId, locale: Locale): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale).format(Instant.ofEpochMilli(then).atZone(zone))

}

/** "Today, 10:42", "Yesterday", "Mon" or a date, following the design's history rows. */
@Composable
fun relativeTimeLabel(then: Long, now: Long, withTime: Boolean = true): String {
    val zone = ZoneId.systemDefault()
    val locale = LocalContext.current.resources.configuration.locales[0]
    return when (RelativeTime.bucket(then, now, zone)) {
        DayBucket.TODAY -> if (withTime) stringResource(R.string.time_today_at, RelativeTime.time(then, zone, locale)) else stringResource(R.string.time_today)
        DayBucket.YESTERDAY -> stringResource(R.string.time_yesterday)
        DayBucket.THIS_WEEK -> RelativeTime.weekday(then, zone, locale)
        DayBucket.OLDER -> RelativeTime.date(then, zone, locale)
    }
}
