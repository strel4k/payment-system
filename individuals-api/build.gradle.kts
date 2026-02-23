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

val nexusUrl      = System.getenv("NEXUS_URL")      ?: findProperty("nexusUrl")?.toString()      ?: "http://localhost:8091/repository/maven-releases/"
val nexusUser     = System.getenv("NEXUS_USERNAME") ?: findProperty("nexusUsername")?.toString() ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: findProperty("nexusPassword")?.toString() ?: "admin123"

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

    val lombokVersion = "1.18.42"

    // Person & Transaction Service clients from Nexus
    implementation("com.example:person-service-api-client:1.0.0")
    implementation("com.example:transaction-service-api-client:1.0.0")

    // --- runtime ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // micrometer + prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

    // OpenTelemetry for tracing
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    // JSON-логирование для Loki
    implementation("net.logstash.logback:logstash-logback-encoder:8.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    // Аннотации javax.annotation (Generated)
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")

    // Аннотации javax.annotation.Nullable / javax.annotation.Nonnull (JSR-305)
    compileOnly("com.google.code.findbugs:jsr305:3.0.2")

    // Для org.openapitools.jackson.nullable.JsonNullableModule
    implementation("org.openapitools:jackson-databind-nullable:0.2.6")

    // --- тесты ---
    testImplementation("org.springframework.boot:spring-boot-starter-test") {
        exclude(group = "org.junit.vintage", module = "junit-vintage-engine")
    }
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.mockito:mockito-core:5.14.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.14.0")
    testImplementation("org.testcontainers:junit-jupiter:1.20.2")
    testImplementation("org.testcontainers:postgresql:1.20.2")
    testImplementation("io.projectreactor:reactor-test")

}

tasks.withType<Test> {
    useJUnitPlatform()
//    enabled = false
}

val integrationTestSourceSet = sourceSets.create("integrationTest") {
    compileClasspath += sourceSets.main.get().output + configurations.testRuntimeClasspath.get()
    runtimeClasspath += output + compileClasspath
    resources.srcDirs("src/integrationTest/resources", "src/test/resources")
    java.srcDir("src/integrationTest/java")
}

configurations[integrationTestSourceSet.implementationConfigurationName].extendsFrom(configurations.testImplementation.get())
configurations[integrationTestSourceSet.runtimeOnlyConfigurationName].extendsFrom(configurations.testRuntimeOnly.get())

val integrationTest = tasks.register<Test>("integrationTest") {
    description = "Runs integration tests."
    group = "verification"
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTest)
}

// ============== OpenAPI генерация DTO ==============
openApiGenerate {
    generatorName.set("java")
    inputSpec.set("$projectDir/openapi/individuals-api.yaml")

    outputDir.set("$buildDir/generated-sources/openapi")

    apiPackage.set("com.example.api")
    modelPackage.set("com.example.dto")

    library.set("webclient")

    generateApiTests.set(false)
    generateModelTests.set(false)
}

sourceSets {
    named("main") {
        java {
            srcDir("$buildDir/generated-sources/openapi/src/main/java")
        }
    }
}

tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}

// JaCoCo configuration
jacoco {
    toolVersion = "0.8.12"
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)

    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }

    classDirectories.setFrom(
        files(classDirectories.files.map {
            fileTree(it) {
                exclude(
                    "**/dto/**",
                    "**/api/**",
                    "**/config/**",
                    "**/IndividualsApiApplication.class",
                    "**/ApiClient*.class",
                    "**/RFC3339DateFormat.class",
                    "**/ServerConfiguration.class",
                    "**/JavaTimeFormatter.class",
                    "**/ServerVariable.class",
                    "**/StringUtil.class",
                    "**/auth/**"
                )
            }
        })
    )
}

tasks.jacocoTestCoverageVerification {
    dependsOn(tasks.jacocoTestReport)

    violationRules {
        rule {
            limit {
                minimum = "0.20".toBigDecimal()
            }
        }

        rule {
            element = "CLASS"
            limit {
                minimum = "0.20".toBigDecimal()
            }

            excludes = listOf(
                "**.dto.*",
                "**.api.*",
                "**.config.*",
                "**.IndividualsApiApplication",
                "**.ApiClient*",
                "**.RFC3339DateFormat",
                "**.ServerConfiguration",
                "**.JavaTimeFormatter",
                "**.ServerVariable",
                "**.StringUtil",
                "**.auth.*",
                "com.example.*",
                "**.exception.GlobalExceptionHandler",
                "**.client.*"
            )
        }
    }
}

tasks.test {
    finalizedBy(tasks.jacocoTestReport)
}

tasks.check {
    dependsOn(tasks.jacocoTestCoverageVerification)
}