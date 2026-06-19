# ─── Stage 1: Build ───
# Usa el Gradle Wrapper del proyecto (Gradle 9.5.1) en lugar de descargar manualmente.
# Esto garantiza que la versión de Gradle sea idéntica a la del entorno local.
FROM eclipse-temurin:21-jdk AS builder

WORKDIR /app

# Copiar wrapper + archivos de build PRIMERO (capa cacheable de dependencias)
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew

# Precachear dependencias (falla sin src pero deja la caché de Gradle lista)
RUN ./gradlew --no-daemon dependencies --quiet || true

# Copiar código fuente y compilar
COPY src ./src
RUN ./gradlew --no-daemon bootJar -x test

# ─── Stage 2: Runtime ───
# eclipse-temurin:21-jre-jammy (Debian) en lugar de Alpine:
# - Resolución DNS más estable en entornos cloud (Supabase, JitPack)
# - Compatibilidad total con herramientas JVM de diagnóstico
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copiar solo el JAR generado (sin fuentes, sin Gradle, sin JDK)
COPY --from=builder /app/build/libs/*.jar app.jar

# Render inyecta $PORT automáticamente; Spring lo lee vía ${PORT:8080}
EXPOSE 8080

# Perfil de producción (usa application-prod.yml)
ENV SPRING_PROFILES_ACTIVE=prod

# Tuning de JVM para contenedores con RAM limitada (Render free/starter)
# MaxRAMPercentage=75% → si Render da 512MB, la JVM usa ~384MB
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:+UseContainerSupport"

# Ejecutar la aplicación
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
