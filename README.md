# A.C.E — Active Challenge Ecosystem

Ecosistema A.C.E. (3 subproyectos locales + librería compartida externa).

## Estructura

- **`:shared` (Externo):** Contratos de datos comunes y enums alojados en `github.com/reinaldojperalta/ace-shared`. Se consume vía JitPack.

| Directorio | Stack | Responsable |
|------------|-------|-------------|
| `ace-backend/` | Spring Boot 4.1.x + Gradle Kotlin DSL + PostgreSQL | Reinaldo, Santiago |
| `ace-mobile/` | Android 13+ (API 33) + Kotlin 2.2.x + Jetpack Compose | Steven Araque |
| `ace-wear/` | Wear OS 3+ (API 30) + Kotlin 2.2.x + Health Services API | Steven Araque |

## Setup y Despliegue

### 1. Backend (`ace-backend`)
- **Base de datos:** PostgreSQL hospedado remotamente en **Supabase**.
- **Ejecución local:** 
  ```bash
  cd ace-backend
  ./gradlew bootRun
  ```
  *(El puerto por defecto es `8080`)*
- **Despliegue:** Se despliega en **Render** usando el `Dockerfile` configurado en el proyecto (con el perfil de producción `application-prod.yml`).

### 2. Mobile / Wear (`ace-mobile`, `ace-wear`)
- Abrir `ace-mobile` y `ace-wear` como proyectos separados en Android Studio.
- Requieren un emulador o dispositivo físico con API 30+ para Wear y API 33+ para Mobile.

## Reglas de Oro

- **Cambios en `:shared` = nuevo tag en su repo + bump de versión en backend, mobile y wear.** Los DTOs son el contrato sagrado.
- **Backend nunca toca carpetas Android.**
- **Mobile/Wear nunca toca `ace-backend/src/`.**
- **Cambios de DB = nueva migración Flyway.** Nunca tocar PostgreSQL directamente sin reflejar el esquema en código.
