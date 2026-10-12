package com.s8202530.assignment2.data

import com.s8202530.assignment2.data.remote.Nit3213Api
import com.s8202530.assignment2.data.repository.AuthRepositoryImpl
import com.s8202530.assignment2.data.repository.DashboardRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Runs the real repositories and Retrofit against a fake server on the test machine. */
class RepositoryTest {

    private lateinit var server: MockWebServer
    private lateinit var api: Nit3213Api

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(Nit3213Api::class.java)
    }

    @After
    fun tearDown() = server.shutdown()

    private fun json(code: Int, body: String) =
        MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test
    fun login_postsToCampusEndpoint_andReturnsKeypass() = runTest {
        server.enqueue(json(200, """{"keypass":"art"}"""))
        val repo = AuthRepositoryImpl(api, campus = "footscray", io = Dispatchers.IO)

        val result = repo.login("8202530", "Manan")

        assertEquals(ApiResult.Success("art"), result)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/footscray/auth", request.path)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"username\":\"8202530\""))
        assertTrue(body.contains("\"password\":\"Manan\""))
    }

    @Test
    fun login_rejected_isWrongCredentials() = runTest {
        server.enqueue(json(400, """{"error":"Invalid credentials"}"""))
        val repo = AuthRepositoryImpl(api, campus = "footscray", io = Dispatchers.IO)

        assertEquals(ApiResult.Error(AppError.WRONG_CREDENTIALS), repo.login("8202530", "wrong"))
    }

    @Test
    fun dashboard_mapsEntitiesWithAnyFieldNames() = runTest {
        server.enqueue(
            json(
                200,
                """{"entities":[
                    {"artistName":"Monet","year":1872,"description":"Painter"},
                    {"artistName":"Van Gogh","year":1889,"description":"Painter"}
                ],"entityTotal":2}""",
            )
        )
        val repo = DashboardRepositoryImpl(api, io = Dispatchers.IO)

        val result = repo.loadDashboard("art")

        assertEquals("/dashboard/art", server.takeRequest().path)
        val data = (result as ApiResult.Success).data
        assertEquals(2, data.total)
        assertEquals("Monet", data.entities.first().title)
        assertEquals("Painter", data.entities.first().description)
    }

    @Test
    fun dashboard_unknownKeypass_isNotFound() = runTest {
        server.enqueue(json(404, """{"error":"Not found"}"""))
        val repo = DashboardRepositoryImpl(api, io = Dispatchers.IO)

        assertEquals(ApiResult.Error(AppError.NOT_FOUND), repo.loadDashboard("nope"))
    }
}
