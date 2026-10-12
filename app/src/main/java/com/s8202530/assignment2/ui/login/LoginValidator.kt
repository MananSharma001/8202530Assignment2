package com.s8202530.assignment2.ui.login

import androidx.annotation.StringRes
import com.s8202530.assignment2.R

/** Checks the form before calling the API. Pure Kotlin, unit tested. */
object LoginValidator {

    private val DIGITS = Regex("^\\d+$")

    data class Result(
        @StringRes val studentIdError: Int?,
        @StringRes val firstNameError: Int?,
    ) {
        val isValid: Boolean get() = studentIdError == null && firstNameError == null
    }

    fun validate(studentId: String, firstName: String): Result {
        val id = studentId.trim()
        val idError = when {
            id.isEmpty() -> R.string.error_student_id_required
            !DIGITS.matches(id) -> R.string.error_student_id_digits // e.g. "s8202530"
            else -> null
        }
        val nameError = if (firstName.isBlank()) R.string.error_first_name_required else null
        return Result(idError, nameError)
    }
}
