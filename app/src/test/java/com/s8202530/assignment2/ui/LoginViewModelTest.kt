package com.s8202530.assignment2.ui

import com.s8202530.assignment2.MainDispatcherRule
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.AppError
import com.s8202530.assignment2.fakes.FakeAuthRepository
import com.s8202530.assignment2.ui.login.LoginViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LoginViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun successfulLogin_sendsKeypass() = runTest {
        val repo = FakeAuthRepository(ApiResult.Success("art"))
        val viewModel = LoginViewModel(repo)

        viewModel.login("8202530", "Manan")

        assertEquals("art", viewModel.loggedIn.first())
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun login_trimsSpacesButKeepsCapitals() = runTest {
        val repo = FakeAuthRepository(ApiResult.Success("art"))
        val viewModel = LoginViewModel(repo)

        viewModel.login(" 8202530 ", " Manan ")

        assertEquals("8202530" to "Manan", repo.calls.single())
    }

    @Test
    fun wrongDetails_showsCredentialsError() = runTest {
        val viewModel = LoginViewModel(FakeAuthRepository(ApiResult.Error(AppError.WRONG_CREDENTIALS)))

        viewModel.login("8202530", "manan")

        assertEquals(R.string.error_wrong_credentials, viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun noInternet_showsNetworkError() = runTest {
        val viewModel = LoginViewModel(FakeAuthRepository(ApiResult.Error(AppError.NO_INTERNET)))

        viewModel.login("8202530", "Manan")

        assertEquals(R.string.error_no_internet, viewModel.state.value.errorMessage)
    }

    @Test
    fun emptyFields_showFieldErrors_andSkipTheApi() = runTest {
        val repo = FakeAuthRepository(ApiResult.Success("art"))
        val viewModel = LoginViewModel(repo)

        viewModel.login("", "")

        assertEquals(R.string.error_student_id_required, viewModel.state.value.studentIdError)
        assertEquals(R.string.error_first_name_required, viewModel.state.value.firstNameError)
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun idWithLetterS_isRejected() = runTest {
        val repo = FakeAuthRepository(ApiResult.Success("art"))
        val viewModel = LoginViewModel(repo)

        viewModel.login("s8202530", "Manan")

        assertEquals(R.string.error_student_id_digits, viewModel.state.value.studentIdError)
        assertTrue(repo.calls.isEmpty())
    }

    @Test
    fun typingAgain_clearsErrors() = runTest {
        val viewModel = LoginViewModel(FakeAuthRepository(ApiResult.Error(AppError.WRONG_CREDENTIALS)))
        viewModel.login("8202530", "x")

        viewModel.onInputChanged()

        assertNull(viewModel.state.value.errorMessage)
    }
}
