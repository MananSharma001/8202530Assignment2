package com.s8202530.assignment2.ui

import androidx.lifecycle.SavedStateHandle
import com.s8202530.assignment2.data.model.AppDetail
import com.s8202530.assignment2.data.model.AppPermission
import com.s8202530.assignment2.data.model.PermissionGroup
import com.s8202530.assignment2.data.repository.PermissionRepository
import com.s8202530.assignment2.data.shizuku.PermissionController
import com.s8202530.assignment2.data.shizuku.ShizukuStatus
import com.s8202530.assignment2.ui.permissions.AppPermissionsActivity
import com.s8202530.assignment2.ui.permissions.AppPermissionsViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
 * The package name comes in through SavedStateHandle, like the keypass on the Dashboard.
 * The repository and the Shizuku controller are both mocked with MockK.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppPermissionsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: PermissionRepository
    private lateinit var controller: PermissionController
    private val shizukuStatus = MutableStateFlow(ShizukuStatus.READY)

    private val location =
        AppPermission("android.permission.ACCESS_FINE_LOCATION", "Precise location", PermissionGroup.LOCATION, granted = true)
    private val camera =
        AppPermission("android.permission.CAMERA", "Take pictures", PermissionGroup.CAMERA, granted = false)

    private val maps = AppDetail(
        packageName = "com.example.maps",
        label = "Maps",
        versionName = "1.0",
        targetSdk = 35,
        isSystem = false,
        permissions = listOf(location, camera),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
        controller = mockk(relaxed = true)
        every { controller.status } returns shizukuStatus
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() = AppPermissionsViewModel(
        repository,
        controller,
        SavedStateHandle(mapOf(AppPermissionsActivity.EXTRA_PACKAGE to "com.example.maps")),
    )

    @Test
    fun startsLoading() {
        assertTrue(createViewModel().state.value.loading)
    }

    @Test
    fun sync_showsThePermissionsForThePackageFromTheIntent() = runTest(testDispatcher) {
        coEvery { repository.loadApp("com.example.maps") } returns maps

        val viewModel = createViewModel()
        viewModel.sync()

        val state = viewModel.state.value
        assertFalse(state.loading)
        assertEquals(maps, state.detail)
        assertEquals(1, state.detail?.grantedCount)
        coVerify(exactly = 1) { repository.loadApp("com.example.maps") }
    }

    @Test
    fun sync_afterUninstall_showsNotFound() = runTest(testDispatcher) {
        coEvery { repository.loadApp(any()) } returns null

        val viewModel = createViewModel()
        viewModel.sync()

        assertTrue(viewModel.state.value.notFound)
        assertNull(viewModel.state.value.detail)
    }

    @Test
    fun toggle_onADeniedPermission_grantsIt_andReadsTheNewState() = runTest(testDispatcher) {
        val afterGrant = maps.copy(permissions = listOf(location, camera.copy(granted = true)))
        coEvery { repository.loadApp(any()) } returns maps andThen afterGrant
        coEvery { controller.grant(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.sync()
        viewModel.toggle(camera)
        assertEquals(setOf(camera.name), viewModel.state.value.busy)
        advanceUntilIdle()

        coVerify(exactly = 1) { controller.grant("com.example.maps", "android.permission.CAMERA") }
        assertEquals(2, viewModel.state.value.detail?.grantedCount)
        assertTrue(viewModel.state.value.busy.isEmpty())
    }

    @Test
    fun toggle_onAnAllowedPermission_revokesIt() = runTest(testDispatcher) {
        coEvery { repository.loadApp(any()) } returns maps
        coEvery { controller.revoke(any(), any()) } returns Result.success(Unit)

        val viewModel = createViewModel()
        viewModel.toggle(location)
        advanceUntilIdle()

        coVerify(exactly = 1) { controller.revoke("com.example.maps", "android.permission.ACCESS_FINE_LOCATION") }
        coVerify(exactly = 0) { controller.grant(any(), any()) }
    }

    @Test
    fun toggle_whenItFails_reportsWhy() = runTest(testDispatcher) {
        coEvery { repository.loadApp(any()) } returns maps
        coEvery { controller.grant(any(), any()) } returns Result.failure(RuntimeException("not changeable"))

        val viewModel = createViewModel()
        viewModel.toggle(camera)
        advanceUntilIdle()

        val failed = viewModel.toggleFailed.first()
        assertEquals("Take pictures", failed.permissionLabel)
        assertEquals("not changeable", failed.reason)
        assertTrue(viewModel.state.value.busy.isEmpty())
    }

    @Test
    fun toggle_withoutShizuku_asksForSetup_andChangesNothing() = runTest(testDispatcher) {
        shizukuStatus.value = ShizukuStatus.NOT_RUNNING

        val viewModel = createViewModel()
        viewModel.toggle(camera)
        advanceUntilIdle()

        assertEquals(ShizukuStatus.NOT_RUNNING, viewModel.needsShizuku.first())
        coVerify(exactly = 0) { controller.grant(any(), any()) }
        coVerify(exactly = 0) { controller.revoke(any(), any()) }
    }
}
