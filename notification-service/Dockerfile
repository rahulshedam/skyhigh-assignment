# Multi-stage build for notification-service
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy and install common module
COPY common /app/common
RUN mvn -f /app/common/pom.xml clean install

# Copy and build notification-service
COPY notification-service /app/notification-service
RUN mvn -f /app/notification-service/pom.xml clean install

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/notification-service/target/*.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
