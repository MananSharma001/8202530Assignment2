package com.s8202530.assignment2.ui.login

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.repository.AuthRepository
import com.s8202530.assignment2.ui.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Login logic. Hilt creates it and passes in the AuthRepository
 * (@HiltViewModel + @Inject constructor).
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = false,
        @StringRes val studentIdError: Int? = null,
        @StringRes val firstNameError: Int? = null,
        @StringRes val errorMessage: Int? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    /** Fires once with the keypass when login succeeds. */
    private val _loggedIn = Channel<String>(Channel.BUFFERED)
    val loggedIn: Flow<String> = _loggedIn.receiveAsFlow()

    fun onInputChanged() {
        val s = _state.value
        if (s.studentIdError != null || s.firstNameError != null || s.errorMessage != null) {
            _state.value = s.copy(studentIdError = null, firstNameError = null, errorMessage = null)
        }
    }

    fun login(studentId: String, firstName: String) {
        if (_state.value.loading) return

        val check = LoginValidator.validate(studentId, firstName)
        if (!check.isValid) {
            _state.value = UiState(studentIdError = check.studentIdError, firstNameError = check.firstNameError)
            return
        }

        _state.value = UiState(loading = true)
        viewModelScope.launch {
            // Only spaces are trimmed; capitals are kept because the API password is case sensitive.
            when (val result = authRepository.login(studentId.trim(), firstName.trim())) {
                is ApiResult.Success -> {
                    _state.value = UiState()
                    _loggedIn.send(result.data)
                }
                is ApiResult.Error -> _state.value = UiState(errorMessage = result.error.messageRes())
            }
        }
    }
}
