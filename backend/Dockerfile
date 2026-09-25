# ==============================================================================
# Multi-stage Dockerfile for CARRY Spring Boot Backend
# ==============================================================================

# Build Stage
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace/app

# Cache dependencies
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x ./mvnw
RUN ./mvnw dependency:go-offline -B || true

# Copy source code and package
COPY src src
RUN ./mvnw clean package -DskipTests

# Run Stage
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app/target/carry-backend-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080 10000

ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
