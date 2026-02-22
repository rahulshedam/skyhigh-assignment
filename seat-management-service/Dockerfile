# Multi-stage build for seat-management-service
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy and install common module
COPY common /app/common
RUN mvn -f /app/common/pom.xml clean install

# Copy and build seat-management-service
COPY seat-management-service /app/seat-management-service
RUN mvn -f /app/seat-management-service/pom.xml clean install

# Runtime stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/seat-management-service/target/*.jar app.jar
RUN mkdir -p /app/data
EXPOSE 8091
ENTRYPOINT ["java", "-jar", "app.jar"]
