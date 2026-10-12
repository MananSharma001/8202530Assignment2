package com.s8202530.assignment2.ui

import androidx.lifecycle.SavedStateHandle
import com.s8202530.assignment2.MainDispatcherRule
import com.s8202530.assignment2.R
import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.AppError
import com.s8202530.assignment2.data.model.DashboardData
import com.s8202530.assignment2.data.model.Entity
import com.s8202530.assignment2.data.model.EntityField
import com.s8202530.assignment2.fakes.FakeDashboardRepository
import com.s8202530.assignment2.ui.dashboard.DashboardActivity
import com.s8202530.assignment2.ui.dashboard.DashboardViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val sample = DashboardData(
        entities = listOf(
            Entity(listOf(EntityField("artistName", "Artist Name", "Monet")), "Impressionist painter"),
            Entity(listOf(EntityField("artistName", "Artist Name", "Van Gogh")), "Post impressionist"),
        ),
        total = 2,
    )

    private fun viewModel(repo: FakeDashboardRepository) =
        DashboardViewModel(repo, SavedStateHandle(mapOf(DashboardActivity.EXTRA_KEYPASS to "art")))

    @Test
    fun loadsWithKeypassFromLogin() {
        val repo = FakeDashboardRepository(ApiResult.Success(sample))

        val vm = viewModel(repo)

        assertEquals("art", vm.keypass)
        assertEquals(listOf("art"), repo.requestedKeypasses)
    }

    @Test
    fun success_showsEntities() {
        val vm = viewModel(FakeDashboardRepository(ApiResult.Success(sample)))

        val state = vm.state.value
        assertTrue(state is DashboardViewModel.UiState.Success)
        assertEquals(2, (state as DashboardViewModel.UiState.Success).data.entities.size)
    }

    @Test
    fun failure_showsErrorMessage() {
        val vm = viewModel(FakeDashboardRepository(ApiResult.Error(AppError.NOT_FOUND)))

        assertEquals(DashboardViewModel.UiState.Error(R.string.error_not_found), vm.state.value)
    }

    @Test
    fun retry_afterError_loadsAgain() {
        val repo = FakeDashboardRepository(ApiResult.Error(AppError.TIMEOUT))
        val vm = viewModel(repo)

        repo.result = ApiResult.Success(sample)
        vm.load()

        assertEquals(2, repo.requestedKeypasses.size)
        assertTrue(vm.state.value is DashboardViewModel.UiState.Success)
    }
}
