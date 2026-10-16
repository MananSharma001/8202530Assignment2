package com.s8202530.assignment2.ui

import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.AppError
import com.s8202530.assignment2.data.repository.AuthRepository
import com.s8202530.assignment2.ui.login.LoginViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * The AuthRepository is mocked with MockK, so these tests never touch the network.
 * viewModelScope runs on Dispatchers.Main, which is swapped for a StandardTestDispatcher;
 * advanceUntilIdle() then runs the queued coroutines before we check the state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: LoginViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        authRepository = mockk()
        viewModel = LoginViewModel(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun successfulLogin_sendsKeypass() = runTest(testDispatcher) {
        coEvery { authRepository.login("8202530", "Manan") } returns ApiResult.Success("art")

        viewModel.login("8202530", "Manan")
        assertTrue(viewModel.state.value.loading) // spinner shows while the call is queued
        advanceUntilIdle()

        assertEquals("art", viewModel.loggedIn.first())
        assertFalse(viewModel.state.value.loading)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun login_trimsSpacesButKeepsCapitals() = runTest(testDispatcher) {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Success("art")

        viewModel.login(" 8202530 ", " Manan ")
        advanceUntilIdle()

        coVerify(exactly = 1) { authRepository.login("8202530", "Manan") }
    }

    @Test
    fun wrongDetails_showsCredentialsError() = runTest(testDispatcher) {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Error(AppError.WRONG_CREDENTIALS)

        viewModel.login("8202530", "manan")
        advanceUntilIdle()

        assertEquals(R.string.error_wrong_credentials, viewModel.state.value.errorMessage)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun noInternet_showsNetworkError() = runTest(testDispatcher) {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Error(AppError.NO_INTERNET)

        viewModel.login("8202530", "Manan")
        advanceUntilIdle()

        assertEquals(R.string.error_no_internet, viewModel.state.value.errorMessage)
    }

    @Test
    fun emptyFields_showFieldErrors_andSkipTheApi() = runTest(testDispatcher) {
        viewModel.login("", "")
        advanceUntilIdle()

        assertEquals(R.string.error_student_id_required, viewModel.state.value.studentIdError)
        assertEquals(R.string.error_first_name_required, viewModel.state.value.firstNameError)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun idWithLetterS_isRejected() = runTest(testDispatcher) {
        viewModel.login("s8202530", "Manan")
        advanceUntilIdle()

        assertEquals(R.string.error_student_id_digits, viewModel.state.value.studentIdError)
        coVerify(exactly = 0) { authRepository.login(any(), any()) }
    }

    @Test
    fun typingAgain_clearsErrors() = runTest(testDispatcher) {
        coEvery { authRepository.login(any(), any()) } returns ApiResult.Error(AppError.WRONG_CREDENTIALS)
        viewModel.login("8202530", "x")
        advanceUntilIdle()

        viewModel.onInputChanged()

        assertNull(viewModel.state.value.errorMessage)
    }
}
