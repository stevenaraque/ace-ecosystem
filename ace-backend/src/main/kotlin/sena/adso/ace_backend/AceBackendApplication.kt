package sena.adso.ace_backend

import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
@Import(FlywayAutoConfiguration::class)
class AceBackendApplication

fun main(args: Array<String>) {
	runApplication<AceBackendApplication>(*args)
}
