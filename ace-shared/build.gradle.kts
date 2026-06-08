plugins {
    kotlin("jvm") version "2.2.21"
    kotlin("plugin.serialization") version "2.2.21"
    `maven-publish`
    `java-library`
}

group = "com.ace"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    withSourcesJar()
    withJavadocJar()
}

// ✅ CORREGIDO: repositorios definidos correctamente
repositories {
    mavenCentral()   // ← Necesario para Kotlin, Gson, kotlinx-serialization, JUnit
    google()         // ← Opcional, por si acaso
}

dependencies {
    // ─── Kotlin Standard Library ───
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.2.21")

    // ─── JSON Serialization: Gson ───
    implementation("com.google.code.gson:gson:2.11.0")

    // ─── JSON Serialization: kotlinx-serialization ───
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    // ─── Testing ───
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.11.0")
    testImplementation("org.assertj:assertj-core:3.26.3")
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.21")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.21")
}

tasks.test {
    useJUnitPlatform()
    failOnNoDiscoveredTests = false
}

// ─── Kotlin 2.2+ compilerOptions DSL ───
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-Xjsr305=strict",
            "-Xannotation-default-target=param-property"
        )
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

// ─── GitHub Packages Publishing ───
publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "shared"  // Esto será parte del nombre final
            version = version.toString()
        }
    }
}