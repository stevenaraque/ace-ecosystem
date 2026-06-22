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
        val uri = request.requestURI

        logger.info("Processing request: $uri, authHeader=${authHeader != null}")

        if (uri.startsWith("/api/auth/")) {
            logger.info("Permitting auth endpoint: $uri")
            filterChain.doFilter(request, response)
            return
        }

        if (authHeader == null || !authHeader.startsWith(AuthConstants.BEARER_PREFIX)) {
            logger.warning("No valid auth header for $uri")
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.removePrefix(AuthConstants.BEARER_PREFIX).trim()
        logger.info("Token extracted for $uri, length=${token.length}")

        val claims = jwtService.validateAccessToken(token)
        logger.info("Token validation result for $uri: claims=${claims != null}")

        if (claims == null) {
            logger.warning("Invalid token for $uri")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"INVALID_TOKEN"}""")
            return
        }

        if (jwtService.isTokenExpired(token)) {
            logger.warning("Token expired for $uri, exp=${claims.expiration}")
            response.status = HttpServletResponse.SC_UNAUTHORIZED
            response.contentType = "application/json"
            response.writer.write("""{"error":"TOKEN_EXPIRED"}""")
            return
        }

        val userId = jwtService.extractUserId(token)
        val deviceId = jwtService.extractDeviceId(token)
        logger.info("Token valid for $uri: userId=$userId, deviceId=$deviceId")

        if (userId != null) {
            val authentication = UsernamePasswordAuthenticationToken(
                userId.toString(),
                null,
                emptyList()
            ).apply {
                details = WebAuthenticationDetailsSource().buildDetails(request)
            }
            SecurityContextHolder.getContext().authentication = authentication
            logger.info("Authentication set for user $userId on $uri")
        } else {
            logger.warning("Could not extract userId from valid token for $uri")
        }

        filterChain.doFilter(request, response)
        logger.info("Request completed: $uri, status=${response.status}")
    }
}