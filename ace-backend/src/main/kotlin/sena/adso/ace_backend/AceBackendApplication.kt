package sena.adso.ace_backend

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication  // sin exclude
class AceBackendApplication

fun main(args: Array<String>) {
    runApplication<AceBackendApplication>(*args)
}
