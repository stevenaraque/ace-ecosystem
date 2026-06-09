package sena.adso.ace_backend.controller

import com.ace.shared.dto.AuthRequestDto
import com.ace.shared.dto.AuthResponseDto
import com.ace.shared.dto.RefreshTokenRequestDto
import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import sena.adso.ace_backend.controller.AuthController 
import sena.adso.ace_backend.service.auth.AuthService

class AuthControllerTest {

    private lateinit var authService: AuthService
    private lateinit var mockMvc: MockMvc
    private val objectMapper = ObjectMapper()

    @BeforeEach
    fun setup() {
        authService = mockk()
        val controller = AuthController(authService)
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build()
    }

    @Test
    fun `POST login - success`() {
        val request = AuthRequestDto("user@ace.com", "pass123", "device-001")
        val response = AuthResponseDto(
            accessToken = "acc",
            refreshToken = "ref",
            expiresIn = 900,
            userId = "uuid",
            email = "user@ace.com"
        )

        every { authService.login(request) } returns response

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { value("acc") }
            jsonPath("$.email") { value("user@ace.com") }
        }

        verify { authService.login(request) }
    }

    @Test
    fun `POST login - invalid credentials`() {
        val request = AuthRequestDto("bad@ace.com", "wrong", "device-001")

        every { authService.login(request) } throws IllegalArgumentException("Invalid credentials")

        mockMvc.post("/api/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `POST register - success`() {
        val request = AuthRequestDto("new@ace.com", "pass123", "device-001")
        val response = AuthResponseDto(
            accessToken = "acc",
            refreshToken = "ref",
            expiresIn = 900,
            userId = "uuid",
            email = "new@ace.com"
        )

        every { authService.register(request) } returns response

        mockMvc.post("/api/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isCreated() }
            jsonPath("$.email") { value("new@ace.com") }
        }
    }

    @Test
    fun `POST refresh - success`() {
        val request = RefreshTokenRequestDto("old-refresh", "device-001")
        val response = AuthResponseDto(
            accessToken = "new-acc",
            refreshToken = "new-ref",
            expiresIn = 900,
            userId = "uuid",
            email = "user@ace.com"
        )

        every { authService.refresh(request) } returns response

        mockMvc.post("/api/auth/refresh") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andExpect {
            status { isOk() }
            jsonPath("$.accessToken") { value("new-acc") }
        }
    }

    @Test
    fun `POST logout - success`() {
        every { authService.logout("refresh-123") } returns Unit

        mockMvc.post("/api/auth/logout") {
            header("X-Refresh-Token", "refresh-123")
        }.andExpect {
            status { isOk() }
        }

        verify { authService.logout("refresh-123") }
    }
}