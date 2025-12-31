import org.gradle.api.tasks.testing.Test

plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.6"
    id("org.openapi.generator") version "7.9.0"
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
    // Версия Lombok — актуальная под новые JDK
    val lombokVersion = "1.18.42"

    // --- runtime ---
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")

    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // micrometer + prometheus
    implementation("io.micrometer:micrometer-registry-prometheus")

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
    resources.srcDir("src/integrationTest/resources")
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

    // можно оставить старый вариант с buildDir — это просто warning о депрекейшене
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

// чтобы перед компиляцией Java всегда генерировались DTO
tasks.named("compileJava") {
    dependsOn("openApiGenerate")
}