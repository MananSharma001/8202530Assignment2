package com.s8202530.assignment2.ui

import com.s8202530.assignment2.R
import com.s8202530.assignment2.ui.login.LoginValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginValidatorTest {

    @Test
    fun validDetails_pass() {
        assertTrue(LoginValidator.validate("8202530", "Manan").isValid)
    }

    @Test
    fun studentIdMustBeDigits() {
        assertEquals(R.string.error_student_id_digits, LoginValidator.validate("s8202530", "Manan").studentIdError)
    }

    @Test
    fun blankFirstName_isRejected() {
        assertEquals(R.string.error_first_name_required, LoginValidator.validate("8202530", "   ").firstNameError)
    }
}
