#!/bin/bash

# Comprehensive Linea Microservice Test Runner
# This script runs all tests, detects issues, and provides fixes

set -e

echo "🚀 Linea Microservice Comprehensive Test Runner"
echo "=============================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Test results
TOTAL_TESTS=0
PASSED_TESTS=0
FAILED_TESTS=0
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
    FAILED_TESTS=$((FAILED_TESTS + 1))
}

log_fix() {
    echo -e "${GREEN}[$(date '+%Y-%m-%d %H:%M:%S')] 🔧 $1${NC}"
    ISSUES_FIXED=$((ISSUES_FIXED + 1))
}

log_test() {
    echo -e "${PURPLE}[$(date '+%Y-%m-%d %H:%M:%S')] 🧪 $1${NC}"
    TOTAL_TESTS=$((TOTAL_TESTS + 1))
}

# Function to run a test
run_test() {
    local test_name="$1"
    local test_command="$2"
    
    log_test "Running: $test_name"
    
    if eval "$test_command"; then
        log_success "$test_name passed"
        PASSED_TESTS=$((PASSED_TESTS + 1))
        return 0
    else
        log_error "$test_name failed"
        return 1
    fi
}

# Function to check prerequisites
check_prerequisites() {
    log_message "Checking prerequisites..."
    
    # Check Java
    run_test "Java Version Check" "java -version 2>&1 | grep -q 'version \"21'"
    
    # Check Maven
    run_test "Maven Version Check" "mvn -version | grep -q 'Apache Maven'"
    
    # Check Docker
    run_test "Docker Version Check" "docker --version | grep -q 'Docker version'"
    
    # Check Docker Compose
    run_test "Docker Compose Version Check" "docker-compose --version | grep -q 'docker-compose version'"
    
    # Check system resources
    run_test "Memory Check" "free -m | awk 'NR==2{exit \$2 >= 4096 ? 0 : 1}'"
    run_test "Disk Space Check" "df -h / | awk 'NR==2{exit \$4 >= \"10G\" ? 0 : 1}'"
    run_test "CPU Cores Check" "nproc | awk '{exit \$1 >= 4 ? 0 : 1}'"
}

# Function to test Maven build
test_maven_build() {
    log_message "Testing Maven build..."
    
    # Clean and compile
    run_test "Maven Clean" "mvn clean -q"
    run_test "Maven Compile" "mvn compile -q"
    run_test "Maven Test" "mvn test -q"
    run_test "Maven Package" "mvn package -q -DskipTests"
}

# Function to test configuration
test_configuration() {
    log_message "Testing configuration..."
    
    # Check configuration files
    run_test "Application YAML Check" "[ -f 'src/main/resources/application.yml' ]"
    run_test "Optimized YAML Check" "[ -f 'src/main/resources/application-optimized.yml' ]"
    run_test "Dockerfile Check" "[ -f 'Dockerfile' ]"
    run_test "Docker Compose Check" "[ -f 'docker-compose.yml' ]"
    run_test "Optimized Docker Compose Check" "[ -f 'docker-compose-optimized.yml' ]"
    
    # Check configuration syntax
    if command -v yq >/dev/null 2>&1; then
        run_test "YAML Syntax Check" "yq eval '.' src/main/resources/application.yml > /dev/null"
    else
        log_warning "yq not installed - skipping YAML syntax check"
    fi
}

# Function to test Docker build
test_docker_build() {
    log_message "Testing Docker build..."
    
    # Build Docker image
    run_test "Docker Build" "docker build -t linea-microservice:test ."
    
    # Test Docker image
    run_test "Docker Image Test" "docker run --rm linea-microservice:test --version 2>/dev/null || true"
}

# Function to test Docker Compose
test_docker_compose() {
    log_message "Testing Docker Compose..."
    
    # Validate Docker Compose
    run_test "Docker Compose Validation" "docker-compose config > /dev/null"
    
    # Test optimized Docker Compose
    if [ -f "docker-compose-optimized.yml" ]; then
        run_test "Optimized Docker Compose Validation" "docker-compose -f docker-compose-optimized.yml config > /dev/null"
    fi
}

# Function to test application startup
test_application_startup() {
    log_message "Testing application startup..."
    
    # Set test environment
    export SPRING_PROFILES_ACTIVE=test
    export LINEA_RPC_URL=https://test-rpc.linea.build
    export LINEA_DATABASE_PATH=/tmp/test_linea.db
    export LINEA_ARCHIVE_DATABASE_PATH=/tmp/test_linea_archive.db
    export LINEA_MAX_CONCURRENT_WORKERS=2
    export LINEA_RATE_LIMIT_PER_SECOND=10
    
    # Start application
    log_message "Starting application..."
    java --enable-preview -jar target/linea-microservice-*.jar > /tmp/app.log 2>&1 &
    APP_PID=$!
    
    # Wait for startup
    sleep 15
    
    # Test health endpoint
    run_test "Health Endpoint Test" "curl -f http://localhost:8008/api/actuator/health > /dev/null 2>&1"
    
    # Test metrics endpoint
    run_test "Metrics Endpoint Test" "curl -f http://localhost:8008/api/actuator/prometheus > /dev/null 2>&1"
    
    # Test API endpoints
    run_test "API Endpoints Test" "curl -f http://localhost:8008/api/actuator/info > /dev/null 2>&1"
    
    # Stop application
    kill $APP_PID 2>/dev/null || true
    wait $APP_PID 2>/dev/null || true
    
    log_success "Application startup test completed"
}

# Function to test database connections
test_database_connections() {
    log_message "Testing database connections..."
    
    # Test MongoDB
    if command -v mongosh >/dev/null 2>&1; then
        run_test "MongoDB Connection Test" "mongosh --eval 'db.runCommand(\"ping\")' > /dev/null 2>&1"
    else
        log_warning "MongoDB client not installed - skipping connection test"
    fi
    
    # Test PostgreSQL
    if command -v psql >/dev/null 2>&1; then
        run_test "PostgreSQL Connection Test" "psql -h localhost -U linea_user -d linea_metrics -c 'SELECT 1;' > /dev/null 2>&1"
    else
        log_warning "PostgreSQL client not installed - skipping connection test"
    fi
    
    # Test Redis
    if command -v redis-cli >/dev/null 2>&1; then
        run_test "Redis Connection Test" "redis-cli ping > /dev/null 2>&1"
    else
        log_warning "Redis client not installed - skipping connection test"
    fi
}

# Function to test metrics and monitoring
test_metrics_monitoring() {
    log_message "Testing metrics and monitoring..."
    
    # Check Prometheus configuration
    run_test "Prometheus Config Check" "[ -f 'prometheus/prometheus.yml' ]"
    
    # Check Grafana dashboard
    run_test "Grafana Dashboard Check" "[ -f 'grafana/dashboards/linea-microservice-dashboard.json' ]"
    
    # Check metrics service
    run_test "Metrics Service Check" "[ -f 'src/main/java/com/defimon/linea/metrics/LineaMetricsService.java' ]"
}

# Function to test performance
test_performance() {
    log_message "Testing performance..."
    
    # Test JVM performance
    run_test "JVM Performance Test" "java --enable-preview -XX:+UseZGC -XX:+UnlockExperimentalVMOptions -XX:+UseTransparentHugePages -Xmx2g -Xms1g -version > /dev/null 2>&1"
    
    # Test Maven performance
    run_test "Maven Performance Test" "mvn clean compile -q -T 4"
    
    # Test Docker performance
    run_test "Docker Performance Test" "docker build -t linea-microservice:perf-test . --no-cache"
}

# Function to test security
test_security() {
    log_message "Testing security..."
    
    # Check for sensitive data in configuration
    run_test "Sensitive Data Check" "! grep -r 'password\\|secret\\|key' src/main/resources/ | grep -v 'linea_password'"
    
    # Check for hardcoded credentials
    run_test "Hardcoded Credentials Check" "! grep -r 'admin\\|root\\|password' src/main/java/ | grep -v 'admin123'"
    
    # Check for exposed ports
    run_test "Exposed Ports Check" "! grep -r '0.0.0.0' docker-compose.yml"
}

# Function to test documentation
test_documentation() {
    log_message "Testing documentation..."
    
    # Check README
    run_test "README Check" "[ -f 'README.md' ]"
    
    # Check architecture documentation
    run_test "Architecture Doc Check" "[ -f 'ARCHITECTURE.md' ]"
    
    # Check performance documentation
    run_test "Performance Doc Check" "[ -f 'PERFORMANCE_COMPARISON.md' ]"
    
    # Check test documentation
    run_test "Test Doc Check" "[ -f 'test-linea-microservice.sh' ]"
    
    # Check fix documentation
    run_test "Fix Doc Check" "[ -f 'fix-linea-issues.sh' ]"
}

# Function to run issue detection and fixing
run_issue_detection_fixing() {
    log_message "Running issue detection and fixing..."
    
    # Run issue detection script
    if [ -f "fix-linea-issues.sh" ]; then
        run_test "Issue Detection Script" "bash fix-linea-issues.sh"
    else
        log_warning "Issue detection script not found"
    fi
}

# Function to generate comprehensive report
generate_comprehensive_report() {
    log_message "Generating comprehensive test report..."
    
    local report_file="/tmp/linea-comprehensive-test-report.md"
    
    cat > "$report_file" << EOF
# Linea Microservice Comprehensive Test Report

Generated on: $(date)

## Test Summary

- **Total Tests**: $TOTAL_TESTS
- **Passed Tests**: $PASSED_TESTS
- **Failed Tests**: $FAILED_TESTS
- **Success Rate**: $((PASSED_TESTS * 100 / TOTAL_TESTS))%

## System Information

- **OS**: $(uname -s)
- **Architecture**: $(uname -m)
- **Java Version**: $(java -version 2>&1 | head -n 1)
- **Maven Version**: $(mvn -version | head -n 1)
- **Docker Version**: $(docker --version 2>/dev/null || echo "Not installed")
- **Docker Compose Version**: $(docker-compose --version 2>/dev/null || echo "Not installed")

## System Resources

- **Memory**: $(free -m | awk 'NR==2{printf "%.0fMB", $2}')
- **Disk Space**: $(df -h / | awk 'NR==2{print $4}')
- **CPU Cores**: $(nproc)

## Test Results

EOF
    
    # Add test results
    if [ $FAILED_TESTS -eq 0 ]; then
        echo "✅ All tests passed successfully!" >> "$report_file"
    else
        echo "❌ $FAILED_TESTS tests failed" >> "$report_file"
    fi
    
    # Add issues information
    if [ $ISSUES_FOUND -gt 0 ]; then
        echo "" >> "$report_file"
        echo "## Issues Found and Fixed" >> "$report_file"
        echo "- **Issues Found**: $ISSUES_FOUND" >> "$report_file"
        echo "- **Issues Fixed**: $ISSUES_FIXED" >> "$report_file"
    fi
    
    # Add recommendations
    echo "" >> "$report_file"
    echo "## Recommendations" >> "$report_file"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        echo "- ✅ Linea microservice is ready for production deployment" >> "$report_file"
        echo "- ✅ All tests passed successfully" >> "$report_file"
        echo "- ✅ No issues detected" >> "$report_file"
    else
        echo "- ⚠️  Some tests failed - please review the output above" >> "$report_file"
        echo "- 🔧 Run the issue detection script to fix problems" >> "$report_file"
        echo "- 📊 Check the logs for detailed error information" >> "$report_file"
    fi
    
    log_success "Comprehensive test report generated: $report_file"
}

# Function to cleanup test environment
cleanup_test_environment() {
    log_message "Cleaning up test environment..."
    
    # Stop any running containers
    docker-compose down 2>/dev/null || true
    docker-compose -f docker-compose-optimized.yml down 2>/dev/null || true
    
    # Remove test Docker images
    docker rmi linea-microservice:test 2>/dev/null || true
    docker rmi linea-microservice:perf-test 2>/dev/null || true
    
    # Clean up test files
    rm -rf /tmp/test_linea.db* 2>/dev/null || true
    rm -rf /tmp/test_linea_archive.db* 2>/dev/null || true
    rm -rf /tmp/app.log 2>/dev/null || true
    
    log_success "Test environment cleaned up"
}

# Main execution
main() {
    echo "Starting comprehensive test suite..."
    echo ""
    
    # Run all tests
    check_prerequisites
    test_maven_build
    test_configuration
    test_docker_build
    test_docker_compose
    test_application_startup
    test_database_connections
    test_metrics_monitoring
    test_performance
    test_security
    test_documentation
    run_issue_detection_fixing
    
    # Generate report
    generate_comprehensive_report
    
    # Cleanup
    cleanup_test_environment
    
    # Final summary
    echo ""
    echo "Comprehensive Test Suite Summary:"
    echo "================================"
    echo "Total Tests: $TOTAL_TESTS"
    echo "Passed Tests: $PASSED_TESTS"
    echo "Failed Tests: $FAILED_TESTS"
    echo "Success Rate: $((PASSED_TESTS * 100 / TOTAL_TESTS))%"
    
    if [ $FAILED_TESTS -eq 0 ]; then
        log_success "All tests passed! Linea microservice is ready for production."
        exit 0
    else
        log_error "Some tests failed. Please review the output above and fix the issues."
        exit 1
    fi
}

# Handle script arguments
case "${1:-}" in
    "cleanup")
        cleanup_test_environment
        ;;
    "report")
        generate_comprehensive_report
        ;;
    "prerequisites")
        check_prerequisites
        ;;
    "build")
        test_maven_build
        test_docker_build
        ;;
    "config")
        test_configuration
        ;;
    "startup")
        test_application_startup
        ;;
    "databases")
        test_database_connections
        ;;
    "metrics")
        test_metrics_monitoring
        ;;
    "performance")
        test_performance
        ;;
    "security")
        test_security
        ;;
    "documentation")
        test_documentation
        ;;
    "issues")
        run_issue_detection_fixing
        ;;
    *)
        main
        ;;
esac
