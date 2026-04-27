import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"
    id("org.openapi.generator") version "7.9.0"
    jacoco
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

val versions = mapOf(
    "lombok"          to "1.18.42",
    "logstash"        to "8.0",
    "mockito"         to "5.14.0",
    "testcontainers"  to "1.20.2",
    "jacoco"          to "0.8.12",
    "avro"            to "1.11.3",
    "kafkaAvro"       to "7.5.0"
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

// ── Nexus credentials ────────────────────────────────────────────
file("${rootDir}/.env").takeIf { it.exists() }?.readLines()?.forEach { line ->
    if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
        val (key, value) = line.split("=", limit = 2)
        System.setProperty(key.trim(), value.trim())
    }
}

val nexusUrl      = System.getenv("NEXUS_URL")      ?: System.getProperty("NEXUS_URL")      ?: "http://localhost:8091/repository/maven-releases/"
val nexusUser     = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME") ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD") ?: "admin123"

// ── OpenAPI генерация ─────────────────────────────────────────────
val openApiSpec = "$projectDir/src/main/resources/openapi/notification-service.yaml"
val generatedDir = layout.buildDirectory.dir("generated-sources/openapi-notification-service")

tasks.register<GenerateTask>("openApiGenerateNotificationServiceApi") {
    generatorName.set("spring")
    inputSpec.set(openApiSpec)
    outputDir.set(generatedDir.get().asFile.absolutePath)

    apiPackage.set("com.example.notificationservice.api")
    modelPackage.set("com.example.dto.notification")
    invokerPackage.set("com.example.notificationservice.invoker")

    configOptions.set(
        mapOf(
            "useSpringBoot3"       to "true",
            "interfaceOnly"        to "true",
            "skipDefaultInterface" to "true",
            "useTags"              to "true",
            "dateLibrary"          to "java8-localdatetime",
            "openApiNullable"      to "false"
        )
    )

    globalProperties.set(
        mapOf(
            "models"          to "",
            "modelDocs"       to "false",
            "modelTests"      to "false",
            "apis"            to "",
            "supportingFiles" to "false"
        )
    )
}

sourceSets {
    main {
        java {
            srcDir(generatedDir.map { it.dir("src/main/java") })
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerateNotificationServiceApi")
}

tasks.named("clean") {
    doLast {
        delete(generatedDir)
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
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
    // ── Web ──────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.25")

    // ── Security ─────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-security")

    // ── Persistence ──────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    // ── Kafka + Avro + Schema Registry ───────────────────────────
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.avro:avro:${versions["avro"]}")
    implementation("io.confluent:kafka-avro-serializer:${versions["kafkaAvro"]}")

    // ── Email ─────────────────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-mail")

    // ── Actuator + Prometheus ─────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // ── Tracing (OTEL) ───────────────────────────────────────────
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    // ── JSON logging ─────────────────────────────────────────────
    implementation("net.logstash.logback:logstash-logback-encoder:${versions["logstash"]}")

    // ── Lombok ───────────────────────────────────────────────────
    compileOnly("org.projectlombok:lombok:${versions["lombok"]}")
    annotationProcessor("org.projectlombok:lombok:${versions["lombok"]}")
    testCompileOnly("org.projectlombok:lombok:${versions["lombok"]}")
    testAnnotationProcessor("org.projectlombok:lombok:${versions["lombok"]}")

    // ── Tests ────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.mockito:mockito-core:${versions["mockito"]}")
    testImplementation("org.mockito:mockito-junit-jupiter:${versions["mockito"]}")
    testImplementation("org.testcontainers:junit-jupiter:${versions["testcontainers"]}")
    testImplementation("org.testcontainers:postgresql:${versions["testcontainers"]}")
    testImplementation("org.testcontainers:kafka:${versions["testcontainers"]}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<Jar>("jar") {
    enabled = false
}

// ── JaCoCo ───────────────────────────────────────────────────────
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
                    "**/config/**",
                    "**/*Application.class",
                    "**/entity/**",
                    "**/invoker/**",
                    "**/dto/**"
                )
            }
        })
    )
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}