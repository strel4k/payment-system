import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    id("org.springframework.boot") version "3.3.5"
    id("io.spring.dependency-management") version "1.1.6"
    id("java")
    id("org.openapi.generator")
    jacoco
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    implementation("org.hibernate.orm:hibernate-envers")
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    runtimeOnly("io.opentelemetry:opentelemetry-exporter-otlp")

    implementation("org.flywaydb:flyway-core")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    implementation("io.swagger.core.v3:swagger-annotations-jakarta:2.2.25")

    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-web")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
}

tasks.test {
    useJUnitPlatform()
}

val openApiSpec = "$projectDir/src/main/resources/openapi/openapi.yml"
val generatedDir = layout.buildDirectory.dir("generated-sources/openapi-person-service")

tasks.register<GenerateTask>("openApiGeneratePersonServiceApi") {
    generatorName.set("spring")
    inputSpec.set(openApiSpec)
    outputDir.set(generatedDir.get().asFile.absolutePath)

    apiPackage.set("com.example.personservice.api")
    modelPackage.set("com.example.dto.person")
    invokerPackage.set("com.example.personservice.invoker")

    configOptions.set(
        mapOf(
            "useSpringBoot3" to "true",
            "interfaceOnly" to "true",
            "skipDefaultInterface" to "false",
            "useTags" to "true",
            "dateLibrary" to "java8",
            "openApiNullable" to "false"
        )
    )

    globalProperties.set(
        mapOf(
            "models" to "",
            "modelDocs" to "false",
            "modelTests" to "false",
            "apis" to "",
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
    dependsOn("openApiGeneratePersonServiceApi")
}

tasks.named("clean") {
    doLast {
        delete(generatedDir)
    }
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
                    "**/PersonServiceApplication.class",
                    "**/entity/CountryEntity.class"
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
                minimum = "0.35".toBigDecimal()
            }
        }

        rule {
            element = "CLASS"
            limit {
                minimum = "0.30".toBigDecimal()
            }

            excludes = listOf(
                "**.dto.*",
                "**.api.*",
                "**.config.*",
                "**.PersonServiceApplication",
                "**.entity.CountryEntity",
                "**.entity.*Entity",
                "**.exception.GlobalExceptionHandler"
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