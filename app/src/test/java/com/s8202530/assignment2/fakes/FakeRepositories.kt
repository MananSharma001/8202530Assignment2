package com.s8202530.assignment2.fakes

import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.model.DashboardData
import com.s8202530.assignment2.data.repository.AuthRepository
import com.s8202530.assignment2.data.repository.DashboardRepository

/** Stands in for the real AuthRepository, so ViewModel tests need no network. */
class FakeAuthRepository(var result: ApiResult<String>) : AuthRepository {
    val calls = mutableListOf<Pair<String, String>>()

    override suspend fun login(studentId: String, firstName: String): ApiResult<String> {
        calls += studentId to firstName
        return result
    }
}

class FakeDashboardRepository(var result: ApiResult<DashboardData>) : DashboardRepository {
    val requestedKeypasses = mutableListOf<String>()

    override suspend fun loadDashboard(keypass: String): ApiResult<DashboardData> {
        requestedKeypasses += keypass
        return result
    }
}
