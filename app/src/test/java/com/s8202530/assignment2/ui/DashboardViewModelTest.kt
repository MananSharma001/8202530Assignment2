package com.s8202530.assignment2.ui

import androidx.lifecycle.SavedStateHandle
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.AppError
import com.s8202530.assignment2.data.model.DashboardData
import com.s8202530.assignment2.data.model.Entity
import com.s8202530.assignment2.data.model.EntityField
import com.s8202530.assignment2.data.repository.DashboardRepository
import com.s8202530.assignment2.ui.dashboard.DashboardActivity
import com.s8202530.assignment2.ui.dashboard.DashboardViewModel
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** DashboardRepository is mocked with MockK; the keypass comes in through SavedStateHandle. */
@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: DashboardRepository

    private val sample = DashboardData(
        entities = listOf(
            Entity(listOf(EntityField("artistName", "Artist Name", "Monet")), "Impressionist painter"),
            Entity(listOf(EntityField("artistName", "Artist Name", "Van Gogh")), "Post impressionist"),
        ),
        total = 2,
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel() =
        DashboardViewModel(repository, SavedStateHandle(mapOf(DashboardActivity.EXTRA_KEYPASS to "art")))

    @Test
    fun startsLoading_thenLoadsWithKeypassFromLogin() = runTest(testDispatcher) {
        coEvery { repository.loadDashboard("art") } returns ApiResult.Success(sample)

        val viewModel = createViewModel()
        assertEquals(DashboardViewModel.UiState.Loading, viewModel.state.value)
        advanceUntilIdle()

        assertEquals("art", viewModel.keypass)
        coVerify(exactly = 1) { repository.loadDashboard("art") }
    }

    @Test
    fun success_showsEntities() = runTest(testDispatcher) {
        coEvery { repository.loadDashboard(any()) } returns ApiResult.Success(sample)

        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.state.value
        assertTrue(state is DashboardViewModel.UiState.Success)
        assertEquals(2, (state as DashboardViewModel.UiState.Success).data.entities.size)
    }

    @Test
    fun failure_showsErrorMessage() = runTest(testDispatcher) {
        coEvery { repository.loadDashboard(any()) } returns ApiResult.Error(AppError.NOT_FOUND)

        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(DashboardViewModel.UiState.Error(R.string.error_not_found), viewModel.state.value)
    }

    @Test
    fun retry_afterError_loadsAgain() = runTest(testDispatcher) {
        coEvery { repository.loadDashboard(any()) } returnsMany listOf(
            ApiResult.Error(AppError.TIMEOUT),
            ApiResult.Success(sample),
        )
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.load()
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.loadDashboard("art") }
        assertTrue(viewModel.state.value is DashboardViewModel.UiState.Success)
    }
}
