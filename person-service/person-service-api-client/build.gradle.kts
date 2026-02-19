plugins {
    `java-library`
    `maven-publish`
    id("org.openapi.generator")
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

val personSpec = "${rootProject.projectDir}/person-service/src/main/resources/openapi/openapi.yml"
val genDir = layout.buildDirectory.dir("generated-sources/openapi-person-models")

tasks.register<org.openapitools.generator.gradle.plugin.tasks.GenerateTask>("generatePersonModels") {
    generatorName.set("java")
    inputSpec.set(personSpec)
    outputDir.set(genDir.get().asFile.absolutePath)

    modelPackage.set("com.example.dto.person")

    globalProperties.set(
        mapOf(
            "models"          to "",
            "apis"            to "false",
            "supportingFiles" to "false"
        )
    )

    additionalProperties.set(
        mapOf(
            "library"              to "resttemplate",
            "serializationLibrary" to "jackson",
            "openApiNullable"      to "false",
            "useJakartaEe"         to "true",
            "dateLibrary"          to "java8",
            "useBeanValidation"    to "true",
            "generateBuilders"     to "false"
        )
    )

    configOptions.set(
        mapOf(
            "serializationLibrary" to "jackson",
            "openApiNullable"      to "false",
            "useJakartaEe"         to "true",
            "dateLibrary"          to "java8",
            "useBeanValidation"    to "true"
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

val nexusUrl      = System.getenv("NEXUS_URL")      ?: findProperty("nexusUrl")?.toString()      ?: "http://localhost:8091/repository/maven-releases/"
val nexusUser     = System.getenv("NEXUS_USERNAME") ?: findProperty("nexusUsername")?.toString() ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: findProperty("nexusPassword")?.toString() ?: "admin123"

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId    = "com.example"
            artifactId = "person-service-api-client"
            version    = "1.0.0"

            from(components["java"])

            pom {
                name.set("Person Service API Client")
                description.set("Auto-generated DTOs from person-service OpenAPI spec")
            }
        }
    }

    repositories {
        maven {
            name = "nexus"
            url  = uri(nexusUrl)
            isAllowInsecureProtocol = true
            credentials {
                username = nexusUser
                password = nexusPassword
            }
        }
        mavenLocal()
    }
}