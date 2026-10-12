package com.s8202530.assignment2.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

/** Every repository call returns one of these instead of throwing. */
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Error(val error: AppError) : ApiResult<Nothing>
}

/** What went wrong, in terms the screens can turn into a message. */
enum class AppError {
    WRONG_CREDENTIALS, // login rejected
    NOT_FOUND,         // unknown keypass
    NO_INTERNET,
    TIMEOUT,           // the free Render server can take a while to wake up
    SERVER,            // 5xx
    UNKNOWN,
}

/**
 * Runs an API call on [dispatcher] and turns every failure into an [AppError].
 * [onHttpError] lets each repository decide what an HTTP code means for it.
 */
suspend fun <T> safeApiCall(
    dispatcher: CoroutineDispatcher,
    onHttpError: (code: Int) -> AppError = ::defaultHttpError,
    call: suspend () -> T,
): ApiResult<T> = withContext(dispatcher) {
    try {
        ApiResult.Success(call())
    } catch (e: CancellationException) {
        throw e
    } catch (e: SocketTimeoutException) {
        ApiResult.Error(AppError.TIMEOUT)
    } catch (e: IOException) {
        ApiResult.Error(AppError.NO_INTERNET)
    } catch (e: HttpException) {
        ApiResult.Error(onHttpError(e.code()))
    } catch (e: Exception) {
        ApiResult.Error(AppError.UNKNOWN)
    }
}

fun defaultHttpError(code: Int): AppError = when {
    code == 404 -> AppError.NOT_FOUND
    code >= 500 -> AppError.SERVER
    else -> AppError.UNKNOWN
}
