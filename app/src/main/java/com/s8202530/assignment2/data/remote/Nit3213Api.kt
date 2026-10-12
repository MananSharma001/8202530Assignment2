package com.s8202530.assignment2.data.remote

import com.s8202530.assignment2.data.remote.dto.DashboardResponse
import com.s8202530.assignment2.data.remote.dto.LoginRequest
import com.s8202530.assignment2.data.remote.dto.LoginResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** The two endpoints of https://nit3213apinew.onrender.com */
interface Nit3213Api {

    /** [campus] is "footscray", "sydney" or "br". */
    @POST("{campus}/auth")
    suspend fun login(
        @Path("campus") campus: String,
        @Body body: LoginRequest,
    ): LoginResponse

    @GET("dashboard/{keypass}")
    suspend fun dashboard(@Path("keypass") keypass: String): DashboardResponse
}
