# ─── Stage 1: Build ───
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app

# Instalar Gradle manualmente
RUN apk add --no-cache curl unzip \
    && curl -L https://services.gradle.org/distributions/gradle-8.14-bin.zip -o gradle.zip \
    && unzip -q gradle.zip \
    && mv gradle-8.14 /opt/gradle \
    && rm gradle.zip
ENV PATH="/opt/gradle/bin:${PATH}"

# Copiar archivos de build
COPY build.gradle.kts .
COPY settings.gradle.kts .
COPY gradle/ gradle/

# Descargar dependencias (capa cacheable)
RUN gradle dependencies --no-daemon

# Copiar código fuente
COPY src/ src/

# Compilar y generar JAR
RUN gradle bootJar --no-daemon

# ─── Stage 2: Runtime ───
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copiar solo el JAR generado
COPY --from=builder /app/build/libs/*.jar app.jar

# Puerto expuesto
EXPOSE 8080

# Ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
