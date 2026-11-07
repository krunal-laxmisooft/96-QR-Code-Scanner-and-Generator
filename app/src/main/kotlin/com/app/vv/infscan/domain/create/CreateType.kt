package com.app.vv.infscan.domain.create

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.app.vv.infscan.R

enum class CreateType(
    @StringRes val title: Int,
    @StringRes val hint: Int,
    @DrawableRes val icon: Int,
) {
    WEBSITE(R.string.create_type_website, R.string.create_hint_website, R.drawable.ic_link),
    TEXT(R.string.create_type_text, R.string.create_hint_text, R.drawable.ic_text),
    WIFI(R.string.create_type_wifi, R.string.create_hint_wifi, R.drawable.ic_wifi),
    CONTACT(R.string.create_type_contact, R.string.create_hint_contact, R.drawable.ic_contact),
    MESSAGE(R.string.create_type_message, R.string.create_hint_message, R.drawable.ic_sms),
    EMAIL(R.string.create_type_email, R.string.create_hint_email, R.drawable.ic_email),
    EVENT(R.string.create_type_event, R.string.create_hint_event, R.drawable.ic_event),
    LOCATION(R.string.create_type_location, R.string.create_hint_location, R.drawable.ic_location),
}
