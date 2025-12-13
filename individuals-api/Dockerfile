# --- Стадия сборки ---
FROM gradle:8.10.2-jdk21-alpine AS build

WORKDIR /workspace/app

# копируем всё внутрь контейнера
COPY . .

# собираем spring-boot jar
RUN gradle bootJar --no-daemon

# --- Стадия рантайма ---
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# копируем собранный jar
COPY --from=build /workspace/app/build/libs/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
