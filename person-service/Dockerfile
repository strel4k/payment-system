# --- build stage ---
FROM gradle:8.14.3-jdk17-alpine AS build

WORKDIR /workspace

# Copy the whole monorepo (docker build context must be repo root)
COPY . .

# Build only person-service
RUN gradle :person-service:bootJar --no-daemon

# --- runtime stage ---
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY --from=build /workspace/person-service/build/libs/*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]
