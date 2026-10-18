package com.s8202530.assignment2.data.repository

import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.model.DashboardData
import com.s8202530.assignment2.data.model.EntityMapper
import com.s8202530.assignment2.data.remote.Nit3213Api
import com.s8202530.assignment2.data.safeApiCall
import com.s8202530.assignment2.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/** Loads the entities for a keypass. An interface so tests can mock it. */
interface DashboardRepository {
    suspend fun loadDashboard(keypass: String): ApiResult<DashboardData>
}

class DashboardRepositoryImpl @Inject constructor(
    private val api: Nit3213Api,
    @IoDispatcher private val io: CoroutineDispatcher,
) : DashboardRepository {

    override suspend fun loadDashboard(keypass: String): ApiResult<DashboardData> {
        val result = safeApiCall(io) { api.dashboard(keypass) }
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val entities = result.data.entities.orEmpty().map(EntityMapper::map)
                ApiResult.Success(DashboardData(entities, total = result.data.entityTotal ?: entities.size))
            }
        }
    }
}
