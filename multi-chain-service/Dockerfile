# Multi-Chain Service Dockerfile
# Java 21 + Spring Boot 3.2 + GraalVM Native Image Support

# Stage 1: Build the application
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven wrapper and pom.xml
COPY mvnw pom.xml ./
COPY .mvn .mvn

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (this layer will be cached if pom.xml doesn't change)
RUN ./mvnw dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN ./mvnw clean package -DskipTests

# Stage 2: Create the runtime image
FROM eclipse-temurin:21-jre-alpine AS runtime

# Install necessary packages
RUN apk add --no-cache \
    curl \
    bash \
    tzdata

# Set timezone
ENV TZ=UTC

# Create application user
RUN addgroup -g 1001 -S multichain && \
    adduser -S multichain -u 1001

# Set working directory
WORKDIR /app

# Copy the built JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Change ownership to multichain user
RUN chown -R multichain:multichain /app

# Switch to multichain user
USER multichain

# Expose port
EXPOSE 8085

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8085/multichain/actuator/health || exit 1

# JVM options for containerized environment
ENV JAVA_OPTS="-Xmx2g -Xms1g \
    -XX:+UseContainerSupport \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseG1GC \
    -XX:+UseStringDeduplication \
    -Djava.security.egd=file:/dev/./urandom \
    -Dspring.profiles.active=docker"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

# Labels for metadata
LABEL maintainer="DEFIMON Team" \
      version="1.0.0" \
      description="Multi-Chain Service with Plugin Architecture" \
      java.version="21" \
      spring.boot.version="3.2.0"
