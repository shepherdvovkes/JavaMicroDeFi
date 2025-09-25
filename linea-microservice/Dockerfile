# Linea Microservice Dockerfile
# Multi-stage build for Java 21 Linea blockchain data collection microservice

# Stage 1: Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder

# Set working directory
WORKDIR /app

# Copy Maven files
COPY pom.xml .
COPY src ./src

# Install Maven
RUN apk add --no-cache maven

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Create directories
RUN mkdir -p /mnt/sata18tb/logs /mnt/sata18tb/linea_backups

# Copy the built JAR
COPY --from=builder /app/target/linea-microservice-*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+UseTransparentHugePages -Xmx2g -Xms1g"
ENV SPRING_PROFILES_ACTIVE=production

# Expose port
EXPOSE 8008

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8008/api/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
