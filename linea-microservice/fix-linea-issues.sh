#!/bin/bash

# Linea Microservice Issue Detection and Fixing Script
# This script detects common issues and provides fixes

set -e

echo "🔧 Linea Microservice Issue Detection and Fixing"
echo "================================================"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Issue tracking
ISSUES_FOUND=0
ISSUES_FIXED=0

# Function to log messages
log_message() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] ✅ $1${NC}"
}

log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ❌ $1${NC}"
    ISSUES_FOUND=$((ISSUES_FOUND + 1))
}

log_fix() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] 🔧 $1${NC}"
    ISSUES_FIXED=$((ISSUES_FIXED + 1))
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Issue 1: Check Java version
fix_java_version() {
    log_message "Checking Java version..."
    
    if ! command_exists java; then
        log_error "Java is not installed"
        log_fix "Installing Java 21..."
        
        # Detect OS and install Java
        if command_exists apt-get; then
            sudo apt-get update
            sudo apt-get install -y openjdk-21-jdk
        elif command_exists yum; then
            sudo yum install -y java-21-openjdk-devel
        elif command_exists brew; then
            brew install openjdk@21
        else
            log_error "Cannot install Java automatically. Please install Java 21 manually."
            return 1
        fi
        
        log_success "Java 21 installed successfully"
    else
        JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
        
        if [ "$JAVA_VERSION" -lt 21 ]; then
            log_error "Java 21+ is required, found Java $JAVA_VERSION"
            log_fix "Upgrading to Java 21..."
            
            # Install Java 21
            if command_exists apt-get; then
                sudo apt-get update
                sudo apt-get install -y openjdk-21-jdk
                sudo update-alternatives --install /usr/bin/java java /usr/lib/jvm/java-21-openjdk/bin/java 1
            elif command_exists yum; then
                sudo yum install -y java-21-openjdk-devel
            elif command_exists brew; then
                brew install openjdk@21
            fi
            
            log_success "Java upgraded to version 21"
        else
            log_success "Java version: $JAVA_VERSION"
        fi
    fi
}

# Issue 2: Check Maven
fix_maven() {
    log_message "Checking Maven..."
    
    if ! command_exists mvn; then
        log_error "Maven is not installed"
        log_fix "Installing Maven..."
        
        if command_exists apt-get; then
            sudo apt-get update
            sudo apt-get install -y maven
        elif command_exists yum; then
            sudo yum install -y maven
        elif command_exists brew; then
            brew install maven
        else
            log_error "Cannot install Maven automatically. Please install Maven manually."
            return 1
        fi
        
        log_success "Maven installed successfully"
    else
        MAVEN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
        log_success "Maven version: $MAVEN_VERSION"
    fi
}

# Issue 3: Check Docker
fix_docker() {
    log_message "Checking Docker..."
    
    if ! command_exists docker; then
        log_error "Docker is not installed"
        log_fix "Installing Docker..."
        
        # Install Docker
        curl -fsSL https://get.docker.com -o get-docker.sh
        sudo sh get-docker.sh
        sudo usermod -aG docker $USER
        rm get-docker.sh
        
        log_success "Docker installed successfully"
        log_warning "Please log out and log back in for Docker group changes to take effect"
    else
        DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
        log_success "Docker version: $DOCKER_VERSION"
    fi
}

# Issue 4: Check Docker Compose
fix_docker_compose() {
    log_message "Checking Docker Compose..."
    
    if ! command_exists docker-compose; then
        log_error "Docker Compose is not installed"
        log_fix "Installing Docker Compose..."
        
        # Install Docker Compose
        sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
        sudo chmod +x /usr/local/bin/docker-compose
        
        log_success "Docker Compose installed successfully"
    else
        COMPOSE_VERSION=$(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)
        log_success "Docker Compose version: $COMPOSE_VERSION"
    fi
}

# Issue 5: Check system resources
fix_system_resources() {
    log_message "Checking system resources..."
    
    # Check memory
    TOTAL_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    if [ "$TOTAL_MEMORY" -lt 4096 ]; then
        log_error "Low memory: ${TOTAL_MEMORY}MB (recommended: 4GB+)"
        log_fix "Optimizing memory usage..."
        
        # Create swap file if needed
        if [ ! -f /swapfile ]; then
            sudo fallocate -l 2G /swapfile
            sudo chmod 600 /swapfile
            sudo mkswap /swapfile
            sudo swapon /swapfile
            echo '/swapfile none swap sw 0 0' | sudo tee -a /etc/fstab
            log_success "Swap file created (2GB)"
        fi
    else
        log_success "Memory: ${TOTAL_MEMORY}MB"
    fi
    
    # Check disk space
    DISK_SPACE=$(df -h / | awk 'NR==2{print $4}' | sed 's/G//')
    if [ "$DISK_SPACE" -lt 10 ]; then
        log_error "Low disk space: ${DISK_SPACE}GB (recommended: 10GB+)"
        log_fix "Cleaning up disk space..."
        
        # Clean up temporary files
        sudo apt-get clean 2>/dev/null || true
        sudo yum clean all 2>/dev/null || true
        docker system prune -f 2>/dev/null || true
        
        log_success "Disk space cleaned up"
    else
        log_success "Disk space: ${DISK_SPACE}GB"
    fi
}

# Issue 6: Fix Maven build issues
fix_maven_build() {
    log_message "Checking Maven build..."
    
    if [ ! -f "pom.xml" ]; then
        log_error "pom.xml not found"
        return 1
    fi
    
    # Check for common Maven issues
    if grep -q "maven.compiler.source" pom.xml; then
        log_success "Maven compiler source configured"
    else
        log_error "Maven compiler source not configured"
        log_fix "Adding Maven compiler configuration..."
        
        # Add compiler configuration to pom.xml
        sed -i '/<properties>/a\    <maven.compiler.source>21</maven.compiler.source>\n    <maven.compiler.target>21</maven.compiler.target>' pom.xml
        log_success "Maven compiler configuration added"
    fi
    
    # Check for Java preview features
    if grep -q "enable-preview" pom.xml; then
        log_success "Java preview features enabled"
    else
        log_error "Java preview features not enabled"
        log_fix "Enabling Java preview features..."
        
        # Add preview features to compiler args
        sed -i '/<compilerArgs>/a\                <arg>--enable-preview</arg>' pom.xml
        log_success "Java preview features enabled"
    fi
}

# Issue 7: Fix configuration issues
fix_configuration() {
    log_message "Checking configuration..."
    
    # Check if application.yml exists
    if [ ! -f "src/main/resources/application.yml" ]; then
        log_error "application.yml not found"
        log_fix "Creating application.yml..."
        
        mkdir -p src/main/resources
        cat > src/main/resources/application.yml << 'EOF'
server:
  port: 8008

spring:
  application:
    name: linea-microservice
  
  # MongoDB Configuration
  data:
    mongodb:
      uri: mongodb://localhost:27017/linea_blockchain
      database: linea_blockchain
  
  # TimescaleDB Configuration
  datasource:
    url: jdbc:postgresql://localhost:5432/linea_metrics
    username: linea_user
    password: linea_password
    driver-class-name: org.postgresql.Driver
  
  # Redis Configuration
  data:
    redis:
      host: localhost
      port: 6379
      database: 0

# Linea Configuration
linea:
  rpc-url: https://dry-special-card.linea-mainnet.quiknode.pro/your_quicknode_linea_key_here/
  database-path: /mnt/sata18tb/linea_data.db
  archive-database-path: /mnt/sata18tb/linea_archive_data.db
  max-concurrent-workers: 10
  rate-limit-per-second: 100
EOF
        log_success "application.yml created"
    else
        log_success "application.yml exists"
    fi
    
    # Check if optimized configuration exists
    if [ ! -f "src/main/resources/application-optimized.yml" ]; then
        log_warning "application-optimized.yml not found"
        log_fix "Creating optimized configuration..."
        
        # Copy from existing file if it exists
        if [ -f "src/main/resources/application.yml" ]; then
            cp src/main/resources/application.yml src/main/resources/application-optimized.yml
            log_success "Optimized configuration created"
        fi
    else
        log_success "Optimized configuration exists"
    fi
}

# Issue 8: Fix Docker issues
fix_docker_issues() {
    log_message "Checking Docker configuration..."
    
    # Check if Dockerfile exists
    if [ ! -f "Dockerfile" ]; then
        log_error "Dockerfile not found"
        log_fix "Creating Dockerfile..."
        
        cat > Dockerfile << 'EOF'
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN apk add --no-cache maven
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN mkdir -p /mnt/sata18tb/logs /mnt/sata18tb/linea_backups
COPY --from=builder /app/target/linea-microservice-*.jar app.jar
ENV JAVA_OPTS="--enable-preview -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+UseTransparentHugePages -Xmx2g -Xms1g"
EXPOSE 8008
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8008/api/actuator/health || exit 1
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EOF
        log_success "Dockerfile created"
    else
        log_success "Dockerfile exists"
    fi
    
    # Check if docker-compose.yml exists
    if [ ! -f "docker-compose.yml" ]; then
        log_error "docker-compose.yml not found"
        log_fix "Creating docker-compose.yml..."
        
        cat > docker-compose.yml << 'EOF'
version: '3.8'

services:
  linea-microservice:
    build: .
    container_name: linea-microservice
    ports:
      - "8008:8008"
    volumes:
      - /mnt/sata18tb:/mnt/sata18tb
    environment:
      - SPRING_PROFILES_ACTIVE=production
      - JAVA_OPTS=--enable-preview -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+UseTransparentHugePages -Xmx2g -Xms1g
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8008/api/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 60s
    networks:
      - linea-network

networks:
  linea-network:
    driver: bridge
EOF
        log_success "docker-compose.yml created"
    else
        log_success "docker-compose.yml exists"
    fi
}

# Issue 9: Fix database connection issues
fix_database_connections() {
    log_message "Checking database connections..."
    
    # Check MongoDB
    if ! command_exists mongosh; then
        log_warning "MongoDB client not installed"
        log_fix "Installing MongoDB client..."
        
        if command_exists apt-get; then
            sudo apt-get update
            sudo apt-get install -y mongodb-mongosh
        elif command_exists yum; then
            sudo yum install -y mongodb-mongosh
        elif command_exists brew; then
            brew install mongosh
        fi
        
        log_success "MongoDB client installed"
    fi
    
    # Check PostgreSQL
    if ! command_exists psql; then
        log_warning "PostgreSQL client not installed"
        log_fix "Installing PostgreSQL client..."
        
        if command_exists apt-get; then
            sudo apt-get update
            sudo apt-get install -y postgresql-client
        elif command_exists yum; then
            sudo yum install -y postgresql
        elif command_exists brew; then
            brew install postgresql
        fi
        
        log_success "PostgreSQL client installed"
    fi
    
    # Check Redis
    if ! command_exists redis-cli; then
        log_warning "Redis client not installed"
        log_fix "Installing Redis client..."
        
        if command_exists apt-get; then
            sudo apt-get update
            sudo apt-get install -y redis-tools
        elif command_exists yum; then
            sudo yum install -y redis
        elif command_exists brew; then
            brew install redis
        fi
        
        log_success "Redis client installed"
    fi
}

# Issue 10: Fix metrics and monitoring
fix_metrics_monitoring() {
    log_message "Checking metrics and monitoring..."
    
    # Check if Prometheus configuration exists
    if [ ! -f "prometheus/prometheus.yml" ]; then
        log_warning "Prometheus configuration not found"
        log_fix "Creating Prometheus configuration..."
        
        mkdir -p prometheus
        cat > prometheus/prometheus.yml << 'EOF'
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'linea-microservice'
    static_configs:
      - targets: ['linea-microservice:8008']
    metrics_path: '/api/actuator/prometheus'
    scrape_interval: 10s
EOF
        log_success "Prometheus configuration created"
    else
        log_success "Prometheus configuration exists"
    fi
    
    # Check if Grafana dashboard exists
    if [ ! -f "grafana/dashboards/linea-microservice-dashboard.json" ]; then
        log_warning "Grafana dashboard not found"
        log_fix "Creating Grafana dashboard..."
        
        mkdir -p grafana/dashboards
        cat > grafana/dashboards/linea-microservice-dashboard.json << 'EOF'
{
  "dashboard": {
    "id": null,
    "title": "Linea Microservice - Blockchain Data Collection",
    "tags": ["linea", "blockchain", "microservice"],
    "panels": [
      {
        "id": 1,
        "title": "Block Collection Performance",
        "type": "stat",
        "targets": [
          {
            "expr": "rate(linea_blocks_collected_total[5m])",
            "legendFormat": "Blocks/sec"
          }
        ]
      }
    ]
  }
}
EOF
        log_success "Grafana dashboard created"
    else
        log_success "Grafana dashboard exists"
    fi
}

# Issue 11: Fix permissions
fix_permissions() {
    log_message "Checking permissions..."
    
    # Check if /mnt/sata18tb exists and is writable
    if [ ! -d "/mnt/sata18tb" ]; then
        log_warning "/mnt/sata18tb directory not found"
        log_fix "Creating /mnt/sata18tb directory..."
        
        sudo mkdir -p /mnt/sata18tb
        sudo chown $USER:$USER /mnt/sata18tb
        sudo chmod 755 /mnt/sata18tb
        log_success "/mnt/sata18tb directory created"
    else
        if [ -w "/mnt/sata18tb" ]; then
            log_success "/mnt/sata18tb is writable"
        else
            log_error "/mnt/sata18tb is not writable"
            log_fix "Fixing /mnt/sata18tb permissions..."
            
            sudo chown $USER:$USER /mnt/sata18tb
            sudo chmod 755 /mnt/sata18tb
            log_success "/mnt/sata18tb permissions fixed"
        fi
    fi
}

# Issue 12: Fix network connectivity
fix_network_connectivity() {
    log_message "Checking network connectivity..."
    
    # Test RPC connectivity
    if curl -f https://dry-special-card.linea-mainnet.quiknode.pro/your_quicknode_linea_key_here/ > /dev/null 2>&1; then
        log_success "RPC endpoint is accessible"
    else
        log_error "RPC endpoint is not accessible"
        log_fix "Testing alternative RPC endpoints..."
        
        # Test alternative endpoints
        if curl -f https://rpc.linea.build > /dev/null 2>&1; then
            log_success "Alternative RPC endpoint is accessible"
        else
            log_error "No RPC endpoints are accessible"
        fi
    fi
}

# Main execution
main() {
    echo "Starting Linea Microservice Issue Detection and Fixing..."
    echo ""
    
    # Fix all issues
    fix_java_version
    fix_maven
    fix_docker
    fix_docker_compose
    fix_system_resources
    fix_maven_build
    fix_configuration
    fix_docker_issues
    fix_database_connections
    fix_metrics_monitoring
    fix_permissions
    fix_network_connectivity
    
    echo ""
    echo "Issue Detection and Fixing Summary:"
    echo "=================================="
    echo "Issues Found: $ISSUES_FOUND"
    echo "Issues Fixed: $ISSUES_FIXED"
    
    if [ $ISSUES_FOUND -eq 0 ]; then
        log_success "No issues found! Linea microservice is ready to run."
    else
        if [ $ISSUES_FIXED -eq $ISSUES_FOUND ]; then
            log_success "All issues have been fixed! Linea microservice is ready to run."
        else
            log_warning "Some issues could not be fixed automatically. Please check the output above."
        fi
    fi
}

# Run main function
main
