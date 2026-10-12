package com.s8202530.assignment2.ui.common

import androidx.annotation.StringRes
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.AppError

/** The message to show the user for each kind of error. */
@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.WRONG_CREDENTIALS -> R.string.error_wrong_credentials
    AppError.NOT_FOUND -> R.string.error_not_found
    AppError.NO_INTERNET -> R.string.error_no_internet
    AppError.TIMEOUT -> R.string.error_timeout
    AppError.SERVER -> R.string.error_server
    AppError.UNKNOWN -> R.string.error_unknown
}
