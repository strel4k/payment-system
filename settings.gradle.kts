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
    "transaction-service:transaction-service-api-client",
    "currency-rate-service",
    "currency-rate-service:currency-rate-service-api-client",
    "fake-payment-provider",
    "payment-service",
    "payment-service:payment-service-api-client",
    "common-library",
    "webhook-collector-service"
)