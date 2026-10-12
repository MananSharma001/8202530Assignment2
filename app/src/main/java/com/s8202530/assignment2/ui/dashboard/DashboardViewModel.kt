package com.s8202530.assignment2.ui.dashboard

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.model.DashboardData
import com.s8202530.assignment2.data.repository.DashboardRepository
import com.s8202530.assignment2.ui.common.messageRes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Loads the entities for the keypass from Login. The keypass arrives in the
 * Activity's Intent and Hilt passes it in through [SavedStateHandle].
 */
@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: DashboardRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data class Success(val data: DashboardData) : UiState
        data class Error(@StringRes val message: Int) : UiState
    }

    val keypass: String = savedStateHandle.get<String>(DashboardActivity.EXTRA_KEYPASS).orEmpty()

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    init {
        load()
    }

    /** Also used by the "Try again" button and pull to refresh. */
    fun load() {
        _state.value = UiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = repository.loadDashboard(keypass)) {
                is ApiResult.Success -> UiState.Success(result.data)
                is ApiResult.Error -> UiState.Error(result.error.messageRes())
            }
        }
    }
}
