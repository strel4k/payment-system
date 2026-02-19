FROM eclipse-temurin:17-jdk-alpine AS build

ARG NEXUS_URL=http://host.docker.internal:8091/repository/maven-releases/
ARG NEXUS_USERNAME=admin
ARG NEXUS_PASSWORD=admin123

ENV NEXUS_URL=${NEXUS_URL}
ENV NEXUS_USERNAME=${NEXUS_USERNAME}
ENV NEXUS_PASSWORD=${NEXUS_PASSWORD}

WORKDIR /home/gradle/project

COPY gradlew gradlew.bat* ./
COPY gradle/ gradle/
RUN chmod +x ./gradlew

COPY settings.gradle.kts build.gradle.kts ./
COPY gradle.properties* ./

COPY person-service/ person-service/
COPY transaction-service/ transaction-service/
COPY individuals-api/ individuals-api/

RUN ./gradlew --no-daemon :transaction-service:transaction-service-api-client:build -x test

RUN ./gradlew --no-daemon :transaction-service:transaction-service-api-client:publish --info

RUN ./gradlew --no-daemon :transaction-service:bootJar -x test

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

ADD https://github.com/open-telemetry/opentelemetry-java-instrumentation/releases/download/v2.10.0/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar
RUN chmod 644 /app/opentelemetry-javaagent.jar

COPY --from=build /home/gradle/project/transaction-service/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=180s --retries=10 \
    CMD wget -qO- http://localhost:8083/actuator/health || exit 1

EXPOSE 8083

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]