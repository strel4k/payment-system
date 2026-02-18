pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        id("org.openapi.generator") version "7.9.0"
    }
}

rootProject.name = "payment-system"

include(
    "individuals-api",
    "person-service",
    "person-service:person-service-api-client",
    "transaction-service",
    "transaction-service:transaction-service-api-client"
)