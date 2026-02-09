rootProject.name = "payment-system"

include(
    "individuals-api",
    "common",
    "person-service"
)

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}
