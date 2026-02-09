# --- build stage ---
FROM gradle:8.14.3-jdk17-alpine AS build
WORKDIR /workspace
COPY . .
RUN ./gradlew :individuals-api:bootJar --no-daemon -x test

# --- runtime stage ---
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY --from=build /workspace/individuals-api/build/libs/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
