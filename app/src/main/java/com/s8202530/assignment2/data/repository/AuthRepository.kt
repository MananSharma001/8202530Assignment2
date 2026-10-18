package com.s8202530.assignment2.data.repository

import com.s8202530.assignment2.data.ApiResult
import com.s8202530.assignment2.data.AppError
import com.s8202530.assignment2.data.defaultHttpError
import com.s8202530.assignment2.data.remote.Nit3213Api
import com.s8202530.assignment2.data.remote.dto.LoginRequest
import com.s8202530.assignment2.data.safeApiCall
import com.s8202530.assignment2.di.CampusLocation
import com.s8202530.assignment2.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

/**
 * Logs in and returns the keypass. An interface, so ViewModel tests can use
 * a MockK mock instead of the real network.
 */
interface AuthRepository {
    suspend fun login(studentId: String, firstName: String): ApiResult<String>
}

class AuthRepositoryImpl @Inject constructor(
    private val api: Nit3213Api,
    @CampusLocation private val campus: String,
    @IoDispatcher private val io: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(studentId: String, firstName: String): ApiResult<String> {
        val result = safeApiCall(io, onHttpError = ::loginHttpError) {
            api.login(campus, LoginRequest(username = studentId, password = firstName))
        }
        return when (result) {
            is ApiResult.Error -> result
            is ApiResult.Success -> {
                val keypass = result.data.keypass
                if (keypass.isNullOrBlank()) ApiResult.Error(AppError.UNKNOWN) else ApiResult.Success(keypass)
            }
        }
    }

    /** The API answers 400, 401 or 404 when the ID or name is wrong. */
    private fun loginHttpError(code: Int): AppError = when (code) {
        400, 401, 403, 404 -> AppError.WRONG_CREDENTIALS
        else -> defaultHttpError(code)
    }
}
