package sena.adso.ace_backend.security

import com.ace.shared.constants.AuthConstants
import sena.adso.ace_backend.auth.service.JwtService
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.util.logging.Logger

@Component
class JwtAuthenticationFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    private val logger = Logger.getLogger(JwtAuthenticationFilter::class.java.name)

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

        val token = authHeader.removePrefix(AuthConstants.BEARER_PREFIX).trim()

        // ← FIX: Validar token PRIMERO, con logging de errores reales
        val claims = jwtService.validateAccessToken(token)
        
        if (claims == null) {
            logger.warning("Invalid token for ${request.requestURI}: signature mismatch or malformed")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"INVALID_TOKEN"}""")
            return
        }

        // ← FIX: Verificar expiración con el claims ya validado
        if (jwtService.isTokenExpired(token)) {
            logger.warning("Token expired for ${request.requestURI}, exp=${claims.expiration}")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
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
            logger.fine("Authenticated user $userId, device $deviceId for ${request.requestURI}")
        }

        filterChain.doFilter(request, response)
    }
}