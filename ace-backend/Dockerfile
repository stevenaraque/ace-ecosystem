# ─── Stage 1: Build ───
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Copiar archivos de Gradle primero (para cachear dependencias)
COPY gradle/ gradle/
COPY gradlew .
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Descargar dependencias (capa cacheable)
RUN ./gradlew dependencies --no-daemon

# Copiar código fuente
COPY src/ src/

# Compilar y generar JAR
RUN ./gradlew bootJar --no-daemon

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar solo el JAR generado
COPY --from=builder /app/build/libs/*.jar app.jar

# Puerto expuesto (Render inyecta PORT, pero documentamos)
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
