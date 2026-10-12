# Stage 1: Build Java application with Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build executable jar
COPY src ./src
RUN mvn clean package -DskipTests -B

# Stage 2: Minimal, secure JRE 21 runtime
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Create upload directory and dedicated non-root user
RUN mkdir -p /app/uploads && \
    useradd -m -u 1001 vynkuser && \
    chown -R vynkuser:vynkuser /app

# Copy built Spring Boot fat JAR from build stage
COPY --from=build --chown=vynkuser:vynkuser /app/target/*.jar app.jar

USER vynkuser
EXPOSE 8080

HEALTHCHECK --interval=20s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/api/csrf || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
