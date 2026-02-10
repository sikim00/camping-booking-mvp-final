package com.example.camp.it

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
abstract class PostgresITBase {

    companion object {

        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test")

        @JvmStatic
        @DynamicPropertySource
        fun props(reg: DynamicPropertyRegistry) {
            reg.add("spring.datasource.url") { postgres.jdbcUrl }
            reg.add("spring.datasource.username") { postgres.username }
            reg.add("spring.datasource.password") { postgres.password }
            reg.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
            reg.add("spring.jpa.hibernate.ddl-auto") { "update" }
            reg.add("spring.jpa.properties.hibernate.dialect") {
                "org.hibernate.dialect.PostgreSQLDialect"
            }
            reg.add("app.jwt.secret") {
                "replace-with-very-long-secret-string-at-least-32-bytes"
            }
        }
    }
}
