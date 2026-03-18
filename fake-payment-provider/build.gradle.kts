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

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

repositories {
    mavenCentral()
}

dependencies {
    val lombokVersion = "1.18.42"

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

    // JSON logging
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // OpenAPI generator annotations
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Test — unit + integration (все в src/test)
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.0")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("org.testcontainers:postgresql:1.20.2")
    testImplementation("org.wiremock:wiremock-standalone:3.3.1")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// ============== OpenAPI генерация DTO ==============
openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$projectDir/openapi/fake-payment-provider.yaml")
    outputDir.set("${layout.buildDirectory.get()}/generated-sources/openapi")
    apiPackage.set("com.example.payment.api")
    modelPackage.set("com.example.payment.dto")
    library.set("resttemplate")
    generateApiTests.set(false)
    generateModelTests.set(false)
    configOptions.set(
        mapOf(
            "useJakartaEe" to "true",
            "openApiNullable" to "true",
            "dateLibrary" to "java8"
        )
    )
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
    toolVersion = "0.8.12"
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
                    "**/FakePaymentProviderApplication.class",
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