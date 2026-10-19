package com.s8202530.assignment2.ui.permissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s8202530.assignment2.data.model.AppDetail
import com.s8202530.assignment2.data.model.AppPermission
import com.s8202530.assignment2.data.repository.PermissionRepository
import com.s8202530.assignment2.data.shizuku.PermissionController
import com.s8202530.assignment2.data.shizuku.ShizukuStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Permissions for one app. The package name arrives in the Activity's Intent
 * and Hilt passes it in through [SavedStateHandle]. Switching a permission
 * goes through [PermissionController] (Shizuku), so it changes right here.
 */
@HiltViewModel
class AppPermissionsViewModel @Inject constructor(
    private val repository: PermissionRepository,
    private val controller: PermissionController,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,
        val detail: AppDetail? = null,
        val notFound: Boolean = false,
        val busy: Set<String> = emptySet(), // permissions being changed right now
    )

    /** Why a switch didn't change, for a snackbar. */
    data class ToggleFailed(val permissionLabel: String, val reason: String)

    val packageName: String = savedStateHandle.get<String>(AppPermissionsActivity.EXTRA_PACKAGE).orEmpty()

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val shizukuStatus: StateFlow<ShizukuStatus> = controller.status

    private val _toggleFailed = Channel<ToggleFailed>(Channel.BUFFERED)
    val toggleFailed: Flow<ToggleFailed> = _toggleFailed.receiveAsFlow()

    /** Sent when a switch is tapped but Shizuku isn't set up yet. */
    private val _needsShizuku = Channel<ShizukuStatus>(Channel.BUFFERED)
    val needsShizuku: Flow<ShizukuStatus> = _needsShizuku.receiveAsFlow()

    /**
     * Reads the app's current permissions. The Activity calls this every
     * couple of seconds while it's on screen, which is what makes it live.
     */
    suspend fun sync() {
        val detail = repository.loadApp(packageName)
        _state.update { it.copy(loading = false, detail = detail, notFound = detail == null) }
    }

    /** Allows the permission if it's off, or takes it away if it's on. */
    fun toggle(permission: AppPermission) {
        if (permission.name in _state.value.busy) return
        controller.refreshStatus()
        val status = controller.status.value
        if (status != ShizukuStatus.READY) {
            viewModelScope.launch { _needsShizuku.send(status) }
            return
        }
        _state.update { it.copy(busy = it.busy + permission.name) }
        viewModelScope.launch {
            val result = if (permission.granted) {
                controller.revoke(packageName, permission.name)
            } else {
                controller.grant(packageName, permission.name)
            }
            result.onFailure { error ->
                _toggleFailed.send(ToggleFailed(permission.label, error.message ?: "unknown error"))
            }
            sync()
            _state.update { it.copy(busy = it.busy - permission.name) }
        }
    }

    fun requestShizukuPermission() = controller.requestPermission()

    fun refreshShizuku() = controller.refreshStatus()
}
