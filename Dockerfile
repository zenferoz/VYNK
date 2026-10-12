# Stage 1: Build Spring Boot Java application from repository root
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies layer
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable fat JAR
COPY backend/src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal Eclipse Temurin JRE 21 runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Install curl for cloud healthcheck and create non-root user
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/* && \
    mkdir -p /app/uploads && \
    useradd -m -u 1001 vynkuser && \
    chown -R vynkuser:vynkuser /app

# Copy built JAR from build stage
COPY --from=build --chown=vynkuser:vynkuser /app/target/*.jar app.jar

USER vynkuser
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
