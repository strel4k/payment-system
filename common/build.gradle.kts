plugins {
    `java-library`
    `maven-publish`
    id("org.openapi.generator") version "7.6.0"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

repositories {
    mavenCentral()
}

dependencies {

    api("com.fasterxml.jackson.core:jackson-annotations:2.17.2")
    api("jakarta.validation:jakarta.validation-api:3.1.0")
    api("jakarta.annotation:jakarta.annotation-api:2.1.1")
}

val personSpec = "$projectDir/src/main/resources/openapi/person-service.yml"
val genDir = layout.buildDirectory.dir("generated-sources/openapi-person-models")

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generatePersonModels") {
    generatorName.set("java")
    inputSpec.set(personSpec)
    outputDir.set(genDir.get().asFile.absolutePath)

    modelPackage.set("com.example.dto.person")

    globalProperties.set(
        mapOf(
            "models" to "",
            "apis" to "false",
            "supportingFiles" to "false"
        )
    )

    additionalProperties.set(
        mapOf(
            "library" to "resttemplate",
            "serializationLibrary" to "jackson",
            "openApiNullable" to "false",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "useBeanValidation" to "true",
            "generateBuilders" to "false"
        )
    )

    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "openApiNullable" to "false",
            "useJakartaEe" to "true",
            "dateLibrary" to "java8",
            "useBeanValidation" to "true"
        )
    )
}

sourceSets {
    named("main") {
        java.srcDir(genDir.map { it.dir("src/main/java") })
    }
}

tasks.named("compileJava") {
    dependsOn("generatePersonModels")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example"
            artifactId = "person-service-client"
            version = "1.0.0"

            from(components["java"])

            pom {
                name.set("Person Service Client")
                description.set("Auto-generated client for person-service API")
            }
        }
    }

    repositories {
        maven {
            name = "nexus"
            url = uri("http://localhost:8091/repository/maven-releases/")
            isAllowInsecureProtocol = true
            credentials {
                username = findProperty("nexusUsername")?.toString() ?: "admin"
                password = findProperty("nexusPassword")?.toString() ?: "admin123"
            }
        }
    }
}