FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY fake-payment-provider/build/libs/*.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
