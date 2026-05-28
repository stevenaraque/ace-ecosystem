# A.C.E — Active Challenge Ecosystem

Monorepo unificado del ecosistema A.C.E.

## Estructura

| Directorio | Stack | Responsable |
|------------|-------|-------------|
| `ace-shared/` | Java 21 (librería pura) | Contratos de datos comunes |
| `ace-backend/` | Spring Boot 4.1.x + Gradle Groovy DSL + PostgreSQL | Reinaldo, Santiago |
| `ace-mobile/` | Android 13+ (API 33) + Java 21 + Jetpack Compose | Steven Araque |
| `ace-wear/` | Wear OS 3+ (API 30) + Java 21 + Health Services API | Steven Araque |

## Setup Rápido (hoy)

### 1. Base de datos local
```bash
docker-compose up -d
```
PostgreSQL en `localhost:5432` (user: `ace`, pass: `ace`, db: `ace_db`).

### 2. Shared (contratos de datos)
```bash
cd ace-shared
./gradlew build publishToMavenLocal
```

### 3. Backend
```bash
cd ace-backend
./gradlew bootRun
```

### 4. Mobile / Wear
Abrir `ace-mobile` y `ace-wear` como proyectos separados en Android Studio. Requieren un emulador/dispositivo con API 30+ (Wear) y 33+ (Mobile).

## Reglas de Oro
- **Nunca editar `ace-shared/` sin avisar a todos.** Los DTOs son el contrato sagrado.
- **Backend nunca toca carpetas Android.**
- **Mobile/Wear nunca toca `ace-backend/src/`.**
- **Cambios de DB = nueva migración Flyway.** Nunca tocar PostgreSQL directamente.
