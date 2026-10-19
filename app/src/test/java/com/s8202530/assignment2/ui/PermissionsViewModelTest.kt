package com.s8202530.assignment2.ui

import com.s8202530.assignment2.data.model.AppSummary
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.data.repository.PermissionRepository
import com.s8202530.assignment2.ui.permissions.AppFilter
import com.s8202530.assignment2.ui.permissions.PermissionsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** PermissionRepository is mocked with MockK, so no real phone is needed. */
@OptIn(ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PermissionRepository

    private val maps = app("Maps", granted = 2, requested = 3, PermissionGroup.LOCATION)
    private val camera = app("Camera", granted = 2, requested = 2, PermissionGroup.CAMERA, PermissionGroup.MICROPHONE)
    private val phone = app("Phone", granted = 1, requested = 4, PermissionGroup.LOCATION, isSystem = true)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        coEvery { repository.loadApps() } returns listOf(camera, maps, phone)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun refresh_loadsApps_andHidesSystemAppsByDefault() = runTest(testDispatcher) {
        val viewModel = PermissionsViewModel(repository)
        assertTrue(viewModel.state.value.loading)

        viewModel.refresh()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(listOf(camera, maps), state.visibleApps)
        coVerify(exactly = 1) { repository.loadApps() }
    }

    @Test
    fun stats_countOnlyAppsYouInstalled() = runTest(testDispatcher) {
        val viewModel = PermissionsViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()

        val stats = viewModel.state.value.stats
        assertEquals(2, stats.appCount)
        assertEquals(4, stats.grantedTotal)
        assertEquals(5, stats.requestedTotal)
        assertEquals(1, stats.locationApps) // Phone is a system app, so it isn't counted
    }

    @Test
    fun filterChip_showsOnlyAppsWithThatPermission() = runTest(testDispatcher) {
        val viewModel = PermissionsViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.setFilter(AppFilter.MICROPHONE)

        assertEquals(listOf(camera), viewModel.state.value.visibleApps)
    }

    @Test
    fun systemSwitch_andSearch_workTogether() = runTest(testDispatcher) {
        val viewModel = PermissionsViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.setShowSystem(true)
        viewModel.setQuery("  pho ")

        assertEquals(listOf(phone), viewModel.state.value.visibleApps)
    }

    @Test
    fun pullToRefresh_scansAgain() = runTest(testDispatcher) {
        val viewModel = PermissionsViewModel(repository)
        viewModel.refresh()
        advanceUntilIdle()

        viewModel.refresh(userInitiated = true)
        advanceUntilIdle()

        assertFalse(viewModel.state.value.refreshing)
        coVerify(exactly = 2) { repository.loadApps() }
    }

    private fun app(
        label: String,
        granted: Int,
        requested: Int,
        vararg groups: PermissionGroup,
        isSystem: Boolean = false,
    ) = AppSummary(
        packageName = "com.example.${label.lowercase()}",
        label = label,
        isSystem = isSystem,
        grantedCount = granted,
        requestedCount = requested,
        grantedGroups = groups.toSet(),
    )
}
