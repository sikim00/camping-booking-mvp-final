import org.gradle.api.tasks.testing.Test

plugins {
    id("base")

    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.6"

    kotlin("jvm") version "1.9.24"
    kotlin("plugin.spring") version "1.9.24"
    kotlin("plugin.jpa") version "1.9.24"
}

group = "com.example"
version = "0.1.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.11.5")

    implementation("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter:1.20.1")
    testImplementation("org.testcontainers:postgresql:1.20.1")

    // ✅ test(profile=test)에서 H2를 쓰기 위해 필요
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    // ✅ 기본 test는 무조건 H2(test 프로파일)
    systemProperty("spring.profiles.active", "test")

    // ✅ Colima + Testcontainers 안정화
    environment(
        "DOCKER_HOST",
        "unix://${System.getProperty("user.home")}/.colima/default/docker.sock"
    )

    // ✅ Ryuk 마운트 이슈 회피(Colima에서 필수)
    environment("TESTCONTAINERS_RYUK_DISABLED", "true")

    // ✅ 기본 test에서는 IT 제외 (필요할 때만 별도 실행)
    filter {
        excludeTestsMatching("*IT")
    }
}
