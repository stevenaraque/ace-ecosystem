# Reporte de Estado: Sistema 4 (Autenticación JWT Híbrida)

Tras analizar el código actual en el módulo `ace-backend` y compararlo con el **Apéndice S4 (ACE_APPENDIX_S4_Auth.md)** y tu tabla de planificación, este es el diagnóstico del sistema:

## 1. Estado del Backend (Pasos 1 - 4)
El backend tiene una base sólida y funcional, pero requiere una corrección crítica de seguridad para cumplir con el diseño arquitectónico.

*   ✅ **Paso 1 (Entidades + Repositorios):** Completado. Existen `User`, `RefreshToken`, `UserRepository` y `RefreshTokenRepository`.
*   ✅ **Paso 2 (Lógica de Negocio):** Completado. `JwtService` y `AuthService` están bien estructurados.
*   ✅ **Paso 4 (API y Filtro):** Completado. `AuthController` expone las rutas correctas y `JwtAuthenticationFilter` verifica correctamente el `Bearer token`, ignorando rutas públicas y retornando `TOKEN_EXPIRED` cuando corresponde.
*   ✅ **Paso 3 (RefreshTokenService con SELECT FOR UPDATE):** Completado. El servicio de rotación está implementado y se añadió el bloqueo `PESSIMISTIC_WRITE` a `RefreshTokenRepository.kt` para prevenir race conditions.

## 2. Estado del Mobile (Pasos 5 - 9)
Confirmando tu intuición, la mayor parte del trabajo restante está en el cliente móvil. Todo el almacenamiento local seguro, la intercepción de red y la UI de login están pendientes.

## 3. Notas Importantes para Coherencia (Backend ↔ Mobile)
Al implementar los pasos del móvil, debes tener en cuenta los siguientes detalles de cómo está programado el backend actualmente para evitar fallos de conexión:

1.  **Endpoint de Logout (Header vs Body):** En el backend, `AuthController.logout` está configurado para recibir el refresh token a través de un **Header** llamado `X-Refresh-Token` (`@RequestHeader("X-Refresh-Token")`), NO en el cuerpo (body) de la petición. El `AuthApi` de Android deberá enviar este header al hacer el logout.
2.  **Identificación de Token Expirado:** Cuando el token de acceso expira, el `JwtAuthenticationFilter` en el backend devuelve un código HTTP `401` con el cuerpo crudo `{"error":"TOKEN_EXPIRED"}`. Tu `AuthInterceptor` en el móvil (Paso 7) deberá leer y parsear este cuerpo de error específico para saber que debe disparar el proceso de refresh, en lugar de un 401 normal.
3.  **Uso del módulo Shared:** El backend utiliza las clases `AuthRequestDto`, `AuthResponseDto` y `RefreshTokenRequestDto` del paquete `com.ace.shared.dto`. El móvil debe estar configurado para importar estas mismas clases y usarlas en las peticiones Retrofit para que el mapeo JSON sea perfecto.

