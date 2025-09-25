#!/bin/bash

# Linea Microservice Test Script
# This script tests the Linea microservice functionality and detects issues

set -e

echo "🚀 Starting Linea Microservice Test Suite"
echo "=========================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Test configuration
TEST_DIR="/tmp/linea_test"
LOG_FILE="$TEST_DIR/test.log"
ERROR_FILE="$TEST_DIR/errors.log"

# Create test directory
mkdir -p "$TEST_DIR"
mkdir -p "$TEST_DIR/logs"
mkdir -p "$TEST_DIR/backups"

# Function to log messages
log_message() {
    echo -e "${BLUE}[$(date '+%Y-%m-%d %H:%M:%S')]${NC} $1" | tee -a "$LOG_FILE"
}

log_success() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] ✅ $1${NC}" | tee -a "$LOG_FILE"
}

log_warning() {
    echo -e "${YELLOW}[$(date '+%Y-%m-%d %H:%M:%S')] ⚠️  $1${NC}" | tee -a "$LOG_FILE"
}

log_error() {
    echo -e "${RED}[$(date '+%Y-%m-%d %H:%M:%S')] ❌ $1${NC}" | tee -a "$LOG_FILE"
    echo "$1" >> "$ERROR_FILE"
}

# Function to check if command exists
command_exists() {
    command -v "$1" >/dev/null 2>&1
}

# Function to check Java version
check_java_version() {
    log_message "Checking Java version..."
    
    if ! command_exists java; then
        log_error "Java is not installed"
        return 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | cut -d'"' -f2 | cut -d'.' -f1)
    
    if [ "$JAVA_VERSION" -lt 21 ]; then
        log_error "Java 21+ is required, found Java $JAVA_VERSION"
        return 1
    fi
    
    log_success "Java version: $JAVA_VERSION"
    return 0
}

# Function to check Maven
check_maven() {
    log_message "Checking Maven..."
    
    if ! command_exists mvn; then
        log_error "Maven is not installed"
        return 1
    fi
    
    MAVEN_VERSION=$(mvn -version | head -n 1 | cut -d' ' -f3)
    log_success "Maven version: $MAVEN_VERSION"
    return 0
}

# Function to check Docker
check_docker() {
    log_message "Checking Docker..."
    
    if ! command_exists docker; then
        log_warning "Docker is not installed - some tests will be skipped"
        return 1
    fi
    
    DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | cut -d',' -f1)
    log_success "Docker version: $DOCKER_VERSION"
    return 0
}

# Function to check Docker Compose
check_docker_compose() {
    log_message "Checking Docker Compose..."
    
    if ! command_exists docker-compose; then
        log_warning "Docker Compose is not installed - some tests will be skipped"
        return 1
    fi
    
    COMPOSE_VERSION=$(docker-compose --version | cut -d' ' -f3 | cut -d',' -f1)
    log_success "Docker Compose version: $COMPOSE_VERSION"
    return 0
}

# Function to check system resources
check_system_resources() {
    log_message "Checking system resources..."
    
    # Check memory
    TOTAL_MEMORY=$(free -m | awk 'NR==2{printf "%.0f", $2}')
    if [ "$TOTAL_MEMORY" -lt 4096 ]; then
        log_warning "Low memory: ${TOTAL_MEMORY}MB (recommended: 4GB+)"
    else
        log_success "Memory: ${TOTAL_MEMORY}MB"
    fi
    
    # Check disk space
    DISK_SPACE=$(df -h / | awk 'NR==2{print $4}' | sed 's/G//')
    if [ "$DISK_SPACE" -lt 10 ]; then
        log_warning "Low disk space: ${DISK_SPACE}GB (recommended: 10GB+)"
    else
        log_success "Disk space: ${DISK_SPACE}GB"
    fi
    
    # Check CPU cores
    CPU_CORES=$(nproc)
    if [ "$CPU_CORES" -lt 4 ]; then
        log_warning "Low CPU cores: $CPU_CORES (recommended: 4+)"
    else
        log_success "CPU cores: $CPU_CORES"
    fi
}

# Function to test Maven build
test_maven_build() {
    log_message "Testing Maven build..."
    
    if [ ! -f "pom.xml" ]; then
        log_error "pom.xml not found"
        return 1
    fi
    
    # Clean and compile
    mvn clean compile -q
    if [ $? -eq 0 ]; then
        log_success "Maven compile successful"
    else
        log_error "Maven compile failed"
        return 1
    fi
    
    # Run tests
    mvn test -q
    if [ $? -eq 0 ]; then
        log_success "Maven tests passed"
    else
        log_error "Maven tests failed"
        return 1
    fi
    
    # Package
    mvn package -q -DskipTests
    if [ $? -eq 0 ]; then
        log_success "Maven package successful"
    else
        log_error "Maven package failed"
        return 1
    fi
}

# Function to test configuration
test_configuration() {
    log_message "Testing configuration..."
    
    # Check if application.yml exists
    if [ ! -f "src/main/resources/application.yml" ]; then
        log_error "application.yml not found"
        return 1
    fi
    
    # Check if application-optimized.yml exists
    if [ ! -f "src/main/resources/application-optimized.yml" ]; then
        log_warning "application-optimized.yml not found"
    else
        log_success "Optimized configuration found"
    fi
    
    # Check configuration syntax
    if command_exists yq; then
        yq eval '.' src/main/resources/application.yml > /dev/null
        if [ $? -eq 0 ]; then
            log_success "Configuration syntax is valid"
        else
            log_error "Configuration syntax is invalid"
            return 1
        fi
    else
        log_warning "yq not installed - skipping configuration syntax check"
    fi
}

# Function to test Docker build
test_docker_build() {
    log_message "Testing Docker build..."
    
    if [ ! -f "Dockerfile" ]; then
        log_error "Dockerfile not found"
        return 1
    fi
    
    # Build Docker image
    docker build -t linea-microservice:test .
    if [ $? -eq 0 ]; then
        log_success "Docker build successful"
    else
        log_error "Docker build failed"
        return 1
    fi
}

# Function to test Docker Compose
test_docker_compose() {
    log_message "Testing Docker Compose..."
    
    if [ ! -f "docker-compose.yml" ]; then
        log_error "docker-compose.yml not found"
        return 1
    fi
    
    if [ ! -f "docker-compose-optimized.yml" ]; then
        log_warning "docker-compose-optimized.yml not found"
    else
        log_success "Optimized Docker Compose found"
    fi
    
    # Validate Docker Compose
    docker-compose config > /dev/null
    if [ $? -eq 0 ]; then
        log_success "Docker Compose configuration is valid"
    else
        log_error "Docker Compose configuration is invalid"
        return 1
    fi
}

# Function to test application startup
test_application_startup() {
    log_message "Testing application startup..."
    
    # Set test environment variables
    export SPRING_PROFILES_ACTIVE=test
    export LINEA_RPC_URL=https://test-rpc.linea.build
    export LINEA_DATABASE_PATH=/tmp/test_linea.db
    export LINEA_ARCHIVE_DATABASE_PATH=/tmp/test_linea_archive.db
    export LINEA_MAX_CONCURRENT_WORKERS=2
    export LINEA_RATE_LIMIT_PER_SECOND=10
    
    # Start application in background
    java --enable-preview -jar target/linea-microservice-*.jar > "$TEST_DIR/app.log" 2>&1 &
    APP_PID=$!
    
    # Wait for application to start
    sleep 10
    
    # Check if application is running
    if ps -p $APP_PID > /dev/null; then
        log_success "Application started successfully (PID: $APP_PID)"
        
        # Test health endpoint
        sleep 5
        if curl -f http://localhost:8008/api/actuator/health > /dev/null 2>&1; then
            log_success "Health endpoint is accessible"
        else
            log_warning "Health endpoint is not accessible"
        fi
        
        # Test metrics endpoint
        if curl -f http://localhost:8008/api/actuator/prometheus > /dev/null 2>&1; then
            log_success "Metrics endpoint is accessible"
        else
            log_warning "Metrics endpoint is not accessible"
        fi
        
        # Stop application
        kill $APP_PID
        wait $APP_PID 2>/dev/null
        log_success "Application stopped successfully"
    else
        log_error "Application failed to start"
        return 1
    fi
}

# Function to test database connections
test_database_connections() {
    log_message "Testing database connections..."
    
    # Test MongoDB connection
    if command_exists mongosh; then
        if mongosh --eval "db.runCommand('ping')" > /dev/null 2>&1; then
            log_success "MongoDB connection successful"
        else
            log_warning "MongoDB connection failed"
        fi
    else
        log_warning "MongoDB client not installed - skipping connection test"
    fi
    
    # Test PostgreSQL connection
    if command_exists psql; then
        if psql -h localhost -U linea_user -d linea_metrics -c "SELECT 1;" > /dev/null 2>&1; then
            log_success "PostgreSQL connection successful"
        else
            log_warning "PostgreSQL connection failed"
        fi
    else
        log_warning "PostgreSQL client not installed - skipping connection test"
    fi
    
    # Test Redis connection
    if command_exists redis-cli; then
        if redis-cli ping > /dev/null 2>&1; then
            log_success "Redis connection successful"
        else
            log_warning "Redis connection failed"
        fi
    else
        log_warning "Redis client not installed - skipping connection test"
    fi
}

# Function to test metrics collection
test_metrics_collection() {
    log_message "Testing metrics collection..."
    
    # Check if Prometheus configuration exists
    if [ ! -f "prometheus/prometheus.yml" ]; then
        log_warning "Prometheus configuration not found"
    else
        log_success "Prometheus configuration found"
    fi
    
    # Check if Grafana dashboard exists
    if [ ! -f "grafana/dashboards/linea-microservice-dashboard.json" ]; then
        log_warning "Grafana dashboard not found"
    else
        log_success "Grafana dashboard found"
    fi
}

# Function to run comprehensive tests
run_comprehensive_tests() {
    log_message "Running comprehensive tests..."
    
    # Test all components
    check_java_version
    check_maven
    check_docker
    check_docker_compose
    check_system_resources
    test_maven_build
    test_configuration
    test_docker_build
    test_docker_compose
    test_application_startup
    test_database_connections
    test_metrics_collection
    
    log_success "Comprehensive tests completed"
}

# Function to generate test report
generate_test_report() {
    log_message "Generating test report..."
    
    REPORT_FILE="$TEST_DIR/test_report.md"
    
    cat > "$REPORT_FILE" << EOF
# Linea Microservice Test Report

Generated on: $(date)

## Test Results

### Prerequisites
- Java Version: $(java -version 2>&1 | head -n 1)
- Maven Version: $(mvn -version | head -n 1)
- Docker Version: $(docker --version 2>/dev/null || echo "Not installed")
- Docker Compose Version: $(docker-compose --version 2>/dev/null || echo "Not installed")

### System Resources
- Memory: $(free -m | awk 'NR==2{printf "%.0fMB", $2}')
- Disk Space: $(df -h / | awk 'NR==2{print $4}')
- CPU Cores: $(nproc)

### Test Results
EOF
    
    if [ -f "$LOG_FILE" ]; then
        echo "## Test Log" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
        cat "$LOG_FILE" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
    fi
    
    if [ -f "$ERROR_FILE" ]; then
        echo "## Errors Found" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
        cat "$ERROR_FILE" >> "$REPORT_FILE"
        echo '```' >> "$REPORT_FILE"
    fi
    
    log_success "Test report generated: $REPORT_FILE"
}

# Function to cleanup test environment
cleanup_test_environment() {
    log_message "Cleaning up test environment..."
    
    # Stop any running containers
    docker-compose down 2>/dev/null || true
    
    # Remove test Docker image
    docker rmi linea-microservice:test 2>/dev/null || true
    
    # Clean up test files
    rm -rf "$TEST_DIR" 2>/dev/null || true
    
    log_success "Test environment cleaned up"
}

# Main execution
main() {
    echo "Starting Linea Microservice Test Suite..."
    echo "Test directory: $TEST_DIR"
    echo "Log file: $LOG_FILE"
    echo "Error file: $ERROR_FILE"
    echo ""
    
    # Run comprehensive tests
    run_comprehensive_tests
    
    # Generate test report
    generate_test_report
    
    # Check for errors
    if [ -f "$ERROR_FILE" ] && [ -s "$ERROR_FILE" ]; then
        echo ""
        log_error "Issues detected during testing:"
        cat "$ERROR_FILE"
        echo ""
        log_error "Test suite completed with errors"
        exit 1
    else
        echo ""
        log_success "Test suite completed successfully"
        exit 0
    fi
}

# Handle script arguments
case "${1:-}" in
    "cleanup")
        cleanup_test_environment
        ;;
    "report")
        generate_test_report
        ;;
    *)
        main
        ;;
esac
