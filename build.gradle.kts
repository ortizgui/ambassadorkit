plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.spring") version "1.9.20"
    id("org.jetbrains.kotlin.plugin.jpa") version "1.9.20"
}

group = "com.company"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "17"
    }
}

repositories {
    mavenCentral()
}

extra["springCloudVersion"] = "2023.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    // Spring Cloud
    implementation("org.springframework.cloud:spring-cloud-starter-openfeign")
    implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    // Resilience4j
    implementation("io.github.resilience4j:resilience4j-spring-boot3")
    implementation("io.github.resilience4j:resilience4j-feign")
    // Rate Limiting - Using a version available in Maven Central
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-core:7.5.0")
    implementation("com.github.vladimir-bukhtoyarov:bucket4j-jcache:7.5.0") // Using jcache instead of Redis
    // Micrometer & Prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")
    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")
    
    // Add SpringDoc OpenAPI for Swagger documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.cloud:spring-cloud-contract-wiremock")
    testImplementation("com.github.tomakehurst:wiremock-jre8:2.35.0")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    // Resilience4j test dependencies
    testImplementation("io.github.resilience4j:resilience4j-test:2.2.0")
    testImplementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")
    // Feign test dependencies
    testImplementation("io.github.openfeign:feign-core:12.5")
    testImplementation("io.github.openfeign:feign-jackson:12.5")
    testImplementation("io.github.openfeign:feign-okhttp:12.5")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Coroutines test
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    // Test containers
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    // Add this line to include the servlet API
    testImplementation("javax.servlet:javax.servlet-api:4.0.1")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
