# Multi-stage build for checkin-service
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy and install common module
COPY common /app/common
RUN mvn -f /app/common/pom.xml clean install

# Copy and build checkin-service
COPY checkin-service /app/checkin-service
RUN mvn -f /app/checkin-service/pom.xml clean install

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/checkin-service/target/*.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
