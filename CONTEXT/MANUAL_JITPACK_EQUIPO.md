# A.C.E — Manual de Trabajo: JitPack y Versionado de `:shared`

> **Para:** Equipo A.C.E (Reinaldo, Santiago, Steven)  
> **Fecha:** Junio 2026  
> **Repo `:shared`:** `https://github.com/reinaldojperalta/ace-shared`  
> **JitPack:** `https://jitpack.io/#reinaldojperalta/ace-shared`

---

## 1. ¿Qué es `:shared`?

`:shared` es el **contrato** entre backend y mobile. Contiene:
- **DTOs** (clases de datos que viajan por la red)
- **Enums** (tipos compartidos como `SportType`, `SessionStatus`)
- **Constantes** (paths, límites, versiones)
- **Serializadores** (Gson + kotlinx-serialization)

**Regla de oro:** Si cambias un campo en `:shared`, **backend y mobile deben usar la misma versión**. Si no, la comunicación se rompe.

---

## 2. ¿Qué es JitPack?

JitPack es un servicio que **compila automáticamente** nuestro código cuando creamos un **tag** (etiqueta de versión) en GitHub.

- No necesitamos configurar servidores Maven
- No necesitamos tokens de GitHub
- Solo: `git tag` → push → JitPack compila → listo para usar

---

## 3. Flujo de Trabajo Completo

### Paso 0: Estructura de Repos

```
GitHub: reinaldojperalta/ace-shared     ← Contratos (DTOs, enums)
         ↓ (publicado vía JitPack)
GitHub: reinaldojperalta/ace-ecosystem  ← Monorepo con backend, mobile, wear
         ├── ace-backend/
         ├── ace-mobile/
         └── ace-wear/
```

**`:shared` vive en repo SEPARADO.** Backend y mobile lo consumen como dependencia Maven.

---

### Paso 1: Iterar Localmente (Sin Tags, Rápido)

Cuando estás **desarrollando solo en tu máquina** y quieres probar cambios en `:shared` sin molestar al equipo:

```bash
# 1. Entra al repo de :shared
cd ~/adso/ace-shared

# 2. Haz tus cambios (ej: agregar un campo a un DTO)
vim src/main/kotlin/com/ace/shared/dto/AuthRequestDto.kt

# 3. Compila y publica en tu máquina local (NO en GitHub, NO tag)
./gradlew publishToMavenLocal
```

```kotlin
// 4. En backend y mobile, usa mavenLocal() para leer el JAR local
// ace-backend/build.gradle.kts o ace-mobile/app/build.gradle.kts

repositories {
    mavenLocal()  // ← Primero, lee de tu disco antes de JitPack
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.0")
    // ↑ Usa la misma versión, pero mavenLocal() tiene prioridad
}
```

```bash
# 5. Compila backend o mobile (usa el JAR local automáticamente)
cd ~/adso/ace-ecosystem/ace-backend
./gradlew build --refresh-dependencies
```

**✅ Ventaja:** Sin tags, sin push, sin esperar. Solo tú ves los cambios.
**⚠️ Limitación:** Solo funciona en TU máquina. Steven y Santiago no ven los cambios.

---

### Paso 2: Publicar para el Equipo (Con Tag, Oficial)

Cuando el cambio está listo y **todos lo necesitan**:

```bash
# 1. Entra al repo de :shared
cd ~/adso/ace-shared

# 2. Asegúrate de que todo compila
./gradlew build

# 3. Commitea tus cambios
git add .
git commit -m "feat: agrega campo deviceType a AuthRequestDto para S4"

# 4. Sube a main
git push origin main

# 5. Crea un TAG (versión semántica)
#    MAJOR.MINOR.PATCH
#    MAJOR = breaking change (campo obligatorio nuevo, renombre)
#    MINOR = adición opcional (campo nuevo con default)
#    PATCH = fix (corrección de adapter, bug)
git tag -a 1.0.1 -m "Release 1.0.1: deviceType en AuthRequestDto"

# 6. Sube el tag a GitHub (esto dispara JitPack)
git push origin 1.0.1
```

```bash
# 7. Verifica que JitPack compiló correctamente
#    Abre en navegador:
#    https://jitpack.io/#reinaldojperalta/ace-shared
#
#    Debes ver 1.0.1 en verde ✅
#    Si está en rojo ❌, revisa el log:
#    https://jitpack.io/com/github/reinaldojperalta/ace-shared/1.0.1/build.log
```

---

### Paso 3: Actualizar Backend y Mobile

**Opción A: Manual (para empezar)**

Edita `build.gradle.kts` en backend y mobile:

```kotlin
// ace-backend/build.gradle.kts
dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.1")  // ← Cambia aquí
}

// ace-mobile/app/build.gradle.kts
dependencies {
    implementation("com.github.reinaldojperalta:ace-shared:1.0.1")  // ← Cambia aquí
}
```

```bash
# Sincroniza Gradle
./gradlew build --refresh-dependencies
```

**Opción B: Version Catalog (recomendado para el equipo)**

Crea `gradle/libs.versions.toml` en backend y mobile:

```toml
# ace-backend/gradle/libs.versions.toml
# ace-mobile/gradle/libs.versions.toml

[versions]
ace-shared = "1.0.1"  # ← Cambias UNA vez aquí

[libraries]
ace-shared = { module = "com.github.reinaldojperalta:ace-shared", version.ref = "ace-shared" }
```

```kotlin
// build.gradle.kts
dependencies {
    implementation(libs.ace.shared)  // ← No hardcodeas la versión
}
```

**Ventaja:** Cambias `1.0.1` → `1.0.2` en **un solo archivo** por proyecto.

---

## 4. Guía Rápida: ¿Qué Hago en Cada Situación?

| Situación | ¿Tag? | ¿Push? | ¿Actualizar backend/mobile? | Comando |
|-----------|-------|--------|------------------------------|---------|
| **Pruebo solo en mi máquina** | No | No | No (usa mavenLocal) | `./gradlew publishToMavenLocal` |
| **Cambio listo, equipo lo necesita** | Sí | Sí | Sí | `git tag -a 1.0.1` + `git push origin 1.0.1` |
| **Fix urgente de bug en DTO** | Sí (PATCH) | Sí | Sí | `git tag -a 1.0.2` + `git push origin 1.0.2` |
| **Nuevo campo opcional** | Sí (MINOR) | Sí | Sí | `git tag -a 1.1.0` + `git push origin 1.1.0` |
| **Renombro campo (breaking)** | Sí (MAJOR) | Sí | Sí (todos deben adaptar) | `git tag -a 2.0.0` + `git push origin 2.0.0` |

---

## 5. Versionado Semántico (SemVer)

```
1.0.0 → 1.0.1 → 1.0.2 → 1.1.0 → 1.1.1 → 2.0.0
  ↑      ↑       ↑       ↑       ↑       ↑
MAJOR  PATCH   PATCH   MINOR   PATCH   MAJOR
```

| Tipo | Ejemplo | Acción del equipo |
|------|---------|-------------------|
| **MAJOR** | Renombrar `AuthRequestDto` → `LoginRequestDto` | **Todos** deben actualizar código y versión |
| **MINOR** | Agregar `deviceType: String? = null` a DTO existente | Actualizar versión, código opcional |
| **PATCH** | Corregir adapter Gson que fallaba con nulls | Actualizar versión, sin cambios de código |

---

## 6. Checklist Antes de Crear un Tag

- [ ] `./gradlew build` pasa sin errores en `:shared`
- [ ] `./gradlew test` pasa (o no hay tests, está OK)
- [ ] Commit con mensaje claro: `feat:`, `fix:`, `refactor:`
- [ ] Push a `main` hecho
- [ ] Tag con versión correcta (SemVer)
- [ ] Push del tag: `git push origin X.Y.Z`
- [ ] JitPack muestra verde ✅ en `https://jitpack.io/#reinaldojperalta/ace-shared`
- [ ] Backend y mobile actualizados a nueva versión
- [ ] `./gradlew build --refresh-dependencies` pasa en backend
- [ ] `./gradlew :app:assembleDebug` pasa en mobile

---

## 7. Solución de Problemas

### JitPack no encuentra la versión
```
Could not find com.github.reinaldojperalta:ace-shared:1.0.1
```

**Causa:** JitPack no compiló todavía o el tag no existe.
**Solución:**
1. Verifica que el tag existe: `git ls-remote --tags origin`
2. Verifica JitPack: `https://jitpack.io/#reinaldojperalta/ace-shared`
3. Si está en rojo, revisa el log: `https://jitpack.io/com/github/reinaldojperalta/ace-shared/1.0.1/build.log`
4. Espera 2 minutos y reintenta: `./gradlew build --refresh-dependencies`

### Backend/mobile no ven el cambio
**Causa:** Gradle cacheó la versión anterior.
**Solución:**
```bash
./gradlew build --refresh-dependencies
# o
rm -rf ~/.gradle/caches/modules-2/files-2.1/com.github.reinaldojperalta*
```

### Quiero volver a una versión anterior
**Solución:** Cambia la versión en `build.gradle.kts` o `libs.versions.toml`:
```kotlin
implementation("com.github.reinaldojperalta:ace-shared:1.0.0")  // Versión anterior
```

---

## 8. Ejemplo Completo: Agregar un Nuevo DTO

**Escenario:** Reinaldo necesita `RefreshTokenRequestDto` para el Sistema 4 (Auth).

### Paso 1: Crear DTO en `:shared`

```bash
cd ~/adso/ace-shared
vim src/main/kotlin/com/ace/shared/dto/RefreshTokenRequestDto.kt
```

```kotlin
package com.ace.shared.dto

import kotlinx.serialization.Serializable

@Serializable
data class RefreshTokenRequestDto(
    val refreshToken: String,
    val deviceId: String
)
```

### Paso 2: Probar localmente

```bash
./gradlew publishToMavenLocal
cd ~/adso/ace-ecosystem/ace-backend
./gradlew build --refresh-dependencies
# Compila OK? Sí. Listo para publicar.
```

### Paso 3: Publicar para el equipo

```bash
cd ~/adso/ace-shared
git add .
git commit -m "feat: agrega RefreshTokenRequestDto para S4"
git push origin main
git tag -a 1.0.1 -m "Release 1.0.1: RefreshTokenRequestDto"
git push origin 1.0.1
```

### Paso 4: Verificar JitPack

Abre `https://jitpack.io/#reinaldojperalta/ace-shared`
Espera a que `1.0.1` esté en verde ✅.

### Paso 5: Actualizar backend y mobile

```kotlin
// ace-backend/build.gradle.kts
implementation("com.github.reinaldojperalta:ace-shared:1.0.1")

// ace-mobile/app/build.gradle.kts
implementation("com.github.reinaldojperalta:ace-shared:1.0.1")
```

```bash
# Backend
cd ~/adso/ace-ecosystem/ace-backend
./gradlew build --refresh-dependencies

# Mobile
cd ~/adso/ace-ecosystem/ace-mobile
./gradlew :app:assembleDebug
```

### Paso 6: Notificar al equipo

En el grupo de WhatsApp/Discord:
> "🚀 `:shared` 1.0.1 publicado. Incluye `RefreshTokenRequestDto` para S4. 
> Actualicen su `build.gradle.kts` o `libs.versions.toml`."

---

## 9. Reglas del Equipo

1. **Nunca modifiques `:shared` dentro de backend o mobile.** Los cambios empiezan en `reinaldojperalta/ace-shared`.
2. **Nunca uses `implementation(files("libs/ace-shared.jar"))`.** Siempre JitPack.
3. **Si es breaking change (MAJOR), avisa ANTES de taggear.** Todos necesitan tiempo para adaptar su código.
4. **Si es MINOR o PATCH, taggea y avisa.** El equipo actualiza cuando pueda.
5. **Si JitPack falla, no taggees de nuevo.** Arregla el error, commitea, y crea un nuevo tag (ej: `1.0.1` falló → arregla → `1.0.2`).

---

## 10. Links Útiles

| Recurso | URL |
|---------|-----|
| Repo `:shared` | `https://github.com/reinaldojperalta/ace-shared` |
| JitPack `:shared` | `https://jitpack.io/#reinaldojperalta/ace-shared` |
| Log de build (ej. 1.0.0) | `https://jitpack.io/com/github/reinaldojperalta/ace-shared/1.0.0/build.log` |
| POM de artifact | `https://jitpack.io/com/github/reinaldojperalta/ace-shared/1.0.0/ace-shared-1.0.0.pom` |
| Docs JitPack | `https://docs.jitpack.io/` |

---

*Documento para el equipo A.C.E. Última actualización: Junio 2026.*
