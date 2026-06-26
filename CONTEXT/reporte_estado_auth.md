# Reporte de Estado: Sistema 4 (Autenticación JWT Híbrida)

> **Actualización 2026-06-25:** S4 está **100% completo**, incluyendo **F7 (JWT único por dispositivo)**: `AuthService.login`/`register` (`auth/service/AuthService.kt:35,68`) ahora llaman `refreshTokenService.revokeAllUserTokens(...)` antes de emitir el nuevo token. Al entrar desde un dispositivo B, el dispositivo A pierde acceso en su próximo `refresh` (`REFRESH_REVOKED` → `AuthInterceptor` limpia tokens → logout automático). El resto del reporte (pasos 1–9) sigue vigente.

Tras analizar el código actual en el módulo `ace-backend` y compararlo con el **Apéndice S4 (ACE_APPENDIX_S4_Auth.md)** y tu tabla de planificación, este es el diagnóstico del sistema:

## 1. Estado del Backend (Pasos 1 - 4)
El backend tiene una base sólida y funcional, pero requiere una corrección crítica de seguridad para cumplir con el diseño arquitectónico.

*   ✅ **Paso 1 (Entidades + Repositorios):** Completado. Existen `User`, `RefreshToken`, `UserRepository` y `RefreshTokenRepository`.
*   ✅ **Paso 2 (Lógica de Negocio):** Completado. `JwtService` y `AuthService` están bien estructurados.
*   ✅ **Paso 4 (API y Filtro):** Completado. `AuthController` expone las rutas correctas y `JwtAuthenticationFilter` verifica correctamente el `Bearer token`, ignorando rutas públicas y retornando `TOKEN_EXPIRED` cuando corresponde.
*   ✅ **Paso 3 (RefreshTokenService con SELECT FOR UPDATE):** Completado. El servicio de rotación está implementado y se añadió el bloqueo `PESSIMISTIC_WRITE` a `RefreshTokenRepository.kt` para prevenir race conditions.

## 2. Estado del Mobile (Pasos 5 - 9)
El cliente móvil tiene una base sólida y funcional.
     ✅ **Paso 5 (Almacenamiento Local Seguro):** Completado. Se integró la persistencia en el dispositivo para salvaguardar el access_token, el refresh_token, el cálculo de token_expires_at y el device_id de forma aislada y protegida.
     ✅ **Paso 6 (UI de Login):** Completado. La pantalla de inicio de sesión está totalmente conectada y consume correctamente el servicio de autenticación inicial.
     ✅ **Paso 7 (AuthInterceptor e Inyección de Red):** Completado. El interceptor añade de forma transparente el header Authorization: Bearer <token> en las rutas privadas y procesa la lógica de renovación automática al detectar el token expirado.
     ✅ **Paso 8 (Control de Race Conditions):** Completado. Se implementó la serialización en el móvil mediante el flag isRefreshing, encolando con éxito las peticiones paralelas para evitar múltiples refrescos  concurrentes.
     ✅ **Paso 9 (Revocación y Logout):** Completado. El flujo de cierre de sesión limpia el estado local y notifica correctamente al servidor.

## 3. Notas Importantes para Coherencia (Backend ↔ Mobile)
Al implementar los pasos del móvil, debes tener en cuenta los siguientes detalles de cómo está programado el backend actualmente para evitar fallos de conexión:

1.  **Endpoint de Logout (Header vs Body):** En el backend, `AuthController.logout` está configurado para recibir el refresh token a través de un **Header** llamado `X-Refresh-Token` (`@RequestHeader("X-Refresh-Token")`), NO en el cuerpo (body) de la petición. El `AuthApi` de Android deberá enviar este header al hacer el logout.
2.  **Identificación de Token Expirado:** Cuando el token de acceso expira, el `JwtAuthenticationFilter` en el backend devuelve un código HTTP `401` con el cuerpo crudo `{"error":"TOKEN_EXPIRED"}`. Tu `AuthInterceptor` en el móvil (Paso 7) deberá leer y parsear este cuerpo de error específico para saber que debe disparar el proceso de refresh, en lugar de un 401 normal.
3.  **Uso del módulo Shared:** El backend utiliza las clases `AuthRequestDto`, `AuthResponseDto` y `RefreshTokenRequestDto` del paquete `com.ace.shared.dto`. El móvil debe estar configurado para importar estas mismas clases y usarlas en las peticiones Retrofit para que el mapeo JSON sea perfecto.

