import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.openapi.generator") version "7.9.0"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

val versions = mapOf(
    "lombok"           to "1.18.42",
    "logstash"         to "8.0",
    "jacksonNullable"  to "0.2.6",
    "javaxAnnotation"  to "1.3.2",
    "mockito"          to "5.14.0",
    "testcontainers"   to "1.20.2",
    "wiremock"         to "3.3.1",
    "jacoco"           to "0.8.12"
)

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

configurations.all {
    resolutionStrategy.cacheChangingModulesFor(0, "seconds")
}

// ============== Nexus credentials ==============
file("${rootDir}/.env").takeIf { it.exists() }?.readLines()?.forEach { line ->
    if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
        val (key, value) = line.split("=", limit = 2)
        System.setProperty(key.trim(), value.trim())
    }
}

val nexusUrl      = System.getenv("NEXUS_URL")      ?: System.getProperty("NEXUS_URL")      ?: "http://localhost:8091/repository/maven-releases/"
val nexusUser     = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME") ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD") ?: "admin123"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        name = "nexus"
        url = uri(nexusUrl)
        isAllowInsecureProtocol = true
        credentials {
            username = nexusUser
            password = nexusPassword
        }
    }
}

dependencies {
    // Web
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // Security — Basic Auth
    implementation("org.springframework.boot:spring-boot-starter-security")

    // Data
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // Actuator + Prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // Tracing — OTEL bridge + exporter
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    // JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash"]}")

    // OpenAPI generated DTO support
    compileOnly("javax.annotation:javax.annotation-api:${versions["javaxAnnotation"]}")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.openapitools:jackson-databind-nullable:${versions["jacksonNullable"]}")

    // Lombok
    compileOnly("org.projectlombok:lombok:${versions["lombok"]}")
    annotationProcessor("org.projectlombok:lombok:${versions["lombok"]}")
    testCompileOnly("org.projectlombok:lombok:${versions["lombok"]}")
    testAnnotationProcessor("org.projectlombok:lombok:${versions["lombok"]}")

    // Tests
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:${versions["mockito"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${versions["mockito"]}")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testcontainers"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testcontainers"]}")
    testImplementation("org.wiremock:wiremock-standalone:${versions["wiremock"]}")
    testImplementation("org.awaitility:awaitility:4.2.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = false
}

// ============== OpenAPI генерация DTO ==============
openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$projectDir/openapi/payment-service.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi")
    apiPackage.set("com.example.paymentservice.api")
    modelPackage.set("com.example.paymentservice.dto")
    library.set("resttemplate")
    generateApiTests.set(false)
    generateModelTests.set(false)
    configOptions.set(mapOf(
        "useJakartaEe"       to "true",
        "openApiNullable"    to "false",
        "dateLibrary"        to "java8",
        "useBeanValidation"  to "true"
    ))
}

sourceSets {
    named("main") {
        java {
            srcDir("${layout.buildDirectory.get()}/generated-sources/openapi/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// ============== JaCoCo ==============
jacoco {
    toolVersion = versions["jacoco"]!!
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    executionData.setFrom(fileTree(layout.buildDirectory).include("jacoco/*.exec"))
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**", "**/api/**", "**/config/**",
                    "**/PaymentServiceApplication.class",
                    "**/ApiClient*.class", "**/RFC3339DateFormat.class",
                    "**/ServerConfiguration.class", "**/StringUtil.class"
                )
            }
        })
    )
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}