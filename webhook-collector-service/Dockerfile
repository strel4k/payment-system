# ─── Stage 1: Build ───────────────────────────────────────────────────────────
FROM gradle:8.14.3-jdk17 AS build

ARG NEXUS_URL=http://host.docker.internal:8091/repository/maven-releases/
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin123

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY settings.gradle.kts build.gradle.kts ./
COPY gradle.properties* ./

COPY common-library/ common-library/

RUN gradle --no-daemon :common-library:build -x test
RUN gradle --no-daemon :common-library:publish --info

COPY webhook-collector-service/ webhook-collector-service/

RUN gradle --no-daemon :webhook-collector-service:bootJar -x test

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

COPY --from=build /home/gradle/project/webhook-collector-service/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- http://localhost:8086/actuator/health || exit 1

EXPOSE 8086

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]