package com.s8202530.assignment2.ui.permissions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s8202530.assignment2.data.model.AppSummary
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.data.model.PermissionStats
import com.s8202530.assignment2.data.repository.PermissionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** The filter chips on the App permissions screen. */
enum class AppFilter(val group: PermissionGroup?) {
    ALL(null),
    LOCATION(PermissionGroup.LOCATION),
    CAMERA(PermissionGroup.CAMERA),
    MICROPHONE(PermissionGroup.MICROPHONE),
}

/**
 * Scans the phone's installed apps and keeps the search, filter chip and
 * system apps switch. The Activity calls [refresh] every time it's shown,
 * so changes made in Android's Settings appear straight away.
 */
@HiltViewModel
class PermissionsViewModel @Inject constructor(
    private val repository: PermissionRepository,
) : ViewModel() {

    data class UiState(
        val loading: Boolean = true,     // first scan, nothing to show yet
        val refreshing: Boolean = false, // pull to refresh
        val allApps: List<AppSummary> = emptyList(),
        val query: String = "",
        val filter: AppFilter = AppFilter.ALL,
        val showSystem: Boolean = false,
    ) {
        /** The summary card counts the apps you installed, not Android's own. */
        val stats: PermissionStats by lazy { PermissionStats.from(allApps.filterNot { it.isSystem }) }

        /** The list after search, filter chip and system apps switch. */
        val visibleApps: List<AppSummary> by lazy {
            allApps.filter { app ->
                (showSystem || !app.isSystem) &&
                    (filter.group == null || filter.group in app.grantedGroups) &&
                    (query.isBlank() ||
                        app.label.contains(query, ignoreCase = true) ||
                        app.packageName.contains(query, ignoreCase = true))
            }
        }
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** [userInitiated] = true for pull to refresh, so the spinner stays until the scan ends. */
    fun refresh(userInitiated: Boolean = false) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = it.allApps.isEmpty(), refreshing = userInitiated) }
            val apps = repository.loadApps()
            _state.update { it.copy(allApps = apps, loading = false, refreshing = false) }
        }
    }

    fun setQuery(query: String) = _state.update { it.copy(query = query.trim()) }

    fun setFilter(filter: AppFilter) = _state.update { it.copy(filter = filter) }

    fun setShowSystem(show: Boolean) = _state.update { it.copy(showSystem = show) }
}
