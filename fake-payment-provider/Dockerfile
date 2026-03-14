FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /home/gradle/project

COPY gradlew gradlew.bat* ./
COPY gradle/ gradle/
RUN chmod +x ./gradlew

COPY settings.gradle.kts build.gradle.kts gradle.properties* ./
COPY fake-payment-provider/ fake-payment-provider/

RUN ./gradlew --no-daemon :fake-payment-provider:bootJar -x test

FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache curl

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=build /home/gradle/project/fake-payment-provider/build/libs/*.jar app.jar

RUN chown -R appuser:appgroup /app
USER appuser

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=10 \
    CMD wget -qO- http://localhost:${SERVER_PORT:-8090}/actuator/health || exit 1

EXPOSE 8090

ENTRYPOINT ["java", "-jar", "app.jar"]