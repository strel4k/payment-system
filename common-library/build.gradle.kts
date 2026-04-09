plugins {
    java
    `maven-publish`
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

val lombokVersion = "1.18.42"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

file("${rootDir}/.env").takeIf { it.exists() }?.readLines()?.forEach { line ->
    if (line.isNotBlank() && !line.startsWith("#") && line.contains("=")) {
        val (key, value) = line.split("=", limit = 2)
        System.setProperty(key.trim(), value.trim())
    }
}

val nexusUrl      = System.getenv("NEXUS_URL")      ?: System.getProperty("NEXUS_URL")      ?: "http://localhost:8091/repository/maven-releases/"
val nexusUser     = System.getenv("NEXUS_USERNAME") ?: System.getProperty("NEXUS_USERNAME") ?: "admin"
val nexusPassword = System.getenv("NEXUS_PASSWORD") ?: System.getProperty("NEXUS_PASSWORD") ?: "admin123"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.example"
            artifactId = "common-library"
            version = project.version.toString()

            from(components["java"])

            pom {
                name.set("Common Library")
                description.set("Shared DTO-модели между сервисами payment-system")
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