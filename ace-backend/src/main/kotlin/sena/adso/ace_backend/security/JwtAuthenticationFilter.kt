package sena.adso.ace_backend.security

import com.ace.shared.constants.AuthConstants
import sena.adso.ace_backend.service.auth.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val authHeader = request.getHeader(AuthConstants.AUTHORIZATION_HEADER)

        if (request.requestURI.startsWith("/api/auth/")) {
            filterChain.doFilter(request, response)
            return
        }

        if (authHeader == null || !authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(AuthConstants.BEARER_PREFIX.length)

        if (jwtService.isTokenExpired(token)) {
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.writer.write("""{"error":"TOKEN_EXPIRED"}""")
            return
        }

        val userId = jwtService.extractUserId(token)
        val deviceId = jwtService.extractDeviceId(token)

        if (userId != null) {
            val authentication = UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                emptyList()
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}