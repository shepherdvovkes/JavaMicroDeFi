#!/bin/bash

# Enhanced Micrometer JVM Metrics Setup Script
# This script sets up comprehensive JVM and business metrics for the DeFi monitoring platform

set -e

echo "🚀 Setting up Enhanced Micrometer JVM Metrics Dashboard"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_header() {
    echo -e "${BLUE}================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}================================${NC}"
}

# Check if Docker is running
check_docker() {
    print_status "Checking Docker status..."
    if ! docker info > /dev/null 2>&1; then
        print_error "Docker is not running. Please start Docker and try again."
        exit 1
    fi
    print_status "Docker is running ✓"
}

# Build the enhanced API Gateway
build_api_gateway() {
    print_header "Building Enhanced API Gateway"
    
    cd api-gateway
    print_status "Building API Gateway with enhanced metrics..."
    
    if [ ! -f "pom.xml" ]; then
        print_error "pom.xml not found in api-gateway directory"
        exit 1
    fi
    
    # Build the application
    mvn clean package -DskipTests
    
    if [ $? -eq 0 ]; then
        print_status "API Gateway built successfully ✓"
    else
        print_error "Failed to build API Gateway"
        exit 1
    fi
    
    cd ..
}

# Start the monitoring stack
start_monitoring() {
    print_header "Starting Monitoring Stack"
    
    print_status "Starting Prometheus and Grafana..."
    
    # Start the monitoring services
    docker-compose up -d prometheus grafana
    
    # Wait for services to be ready
    print_status "Waiting for services to start..."
    sleep 10
    
    # Check if Prometheus is ready
    print_status "Checking Prometheus health..."
    if curl -s http://localhost:9091/-/healthy > /dev/null; then
        print_status "Prometheus is healthy ✓"
    else
        print_warning "Prometheus health check failed, but continuing..."
    fi
    
    # Check if Grafana is ready
    print_status "Checking Grafana health..."
    if curl -s http://localhost:3000/api/health > /dev/null; then
        print_status "Grafana is healthy ✓"
    else
        print_warning "Grafana health check failed, but continuing..."
    fi
}

# Import the JVM Metrics Dashboard
import_dashboard() {
    print_header "Importing JVM Metrics Dashboard"
    
    print_status "Waiting for Grafana to be fully ready..."
    sleep 15
    
    # Import the dashboard
    print_status "Importing JVM Metrics Dashboard..."
    
    if [ -f "api-gateway/basic-jvm-metrics-dashboard.json" ]; then
        # Use Grafana API to import dashboard
        curl -X POST \
            -H "Content-Type: application/json" \
            -H "Authorization: Basic YWRtaW46ZGVmaW1vbjEyMw==" \
            -d @api-gateway/basic-jvm-metrics-dashboard.json \
            http://localhost:3000/api/dashboards/db
        
        if [ $? -eq 0 ]; then
            print_status "Basic JVM Metrics Dashboard imported successfully ✓"
        else
            print_warning "Failed to import dashboard via API, but file is ready for manual import"
        fi
    else
        print_error "Dashboard JSON file not found"
        exit 1
    fi
}

# Start the API Gateway
start_api_gateway() {
    print_header "Starting Enhanced API Gateway"
    
    print_status "Starting API Gateway with enhanced metrics..."
    
    # Start the API Gateway
    docker-compose up -d api-gateway
    
    # Wait for the service to start
    print_status "Waiting for API Gateway to start..."
    sleep 15
    
    # Check if the metrics endpoint is accessible
    print_status "Checking API Gateway metrics endpoint..."
    if curl -s http://localhost:8080/actuator/prometheus > /dev/null; then
        print_status "API Gateway metrics endpoint is accessible ✓"
    else
        print_warning "API Gateway metrics endpoint not accessible yet, but service may still be starting..."
    fi
    
    # Check health endpoint
    print_status "Checking API Gateway health..."
    if curl -s http://localhost:8080/actuator/health > /dev/null; then
        print_status "API Gateway is healthy ✓"
    else
        print_warning "API Gateway health check failed, but continuing..."
    fi
}

# Verify metrics collection
verify_metrics() {
    print_header "Verifying Metrics Collection"
    
    print_status "Checking Prometheus targets..."
    
    # Wait a bit for metrics to be collected
    sleep 30
    
    # Check if API Gateway is being scraped
    if curl -s "http://localhost:9091/api/v1/targets" | grep -q "api-gateway"; then
        print_status "API Gateway target found in Prometheus ✓"
    else
        print_warning "API Gateway target not found in Prometheus"
    fi
    
    # Check for specific JVM metrics
    print_status "Checking for JVM metrics..."
    if curl -s "http://localhost:9091/api/v1/query?query=jvm_memory_used_bytes" | grep -q "result"; then
        print_status "JVM memory metrics found ✓"
    else
        print_warning "JVM memory metrics not found yet"
    fi
    
    # Check for custom business metrics
    print_status "Checking for business metrics..."
    if curl -s "http://localhost:9091/api/v1/query?query=gateway_requests_total" | grep -q "result"; then
        print_status "Gateway metrics found ✓"
    else
        print_warning "Gateway metrics not found yet"
    fi
}

# Display access information
show_access_info() {
    print_header "Access Information"
    
    echo -e "${GREEN}🎯 Dashboard Access:${NC}"
    echo -e "  • Grafana: http://localhost:3000"
    echo -e "    Username: admin"
    echo -e "    Password: defimon123"
    echo ""
    echo -e "  • Prometheus: http://localhost:9091"
    echo ""
    echo -e "  • API Gateway Metrics: http://localhost:8080/actuator/prometheus"
    echo -e "  • API Gateway Health: http://localhost:8080/actuator/health"
    echo ""
    echo -e "${GREEN}📊 Available Dashboards:${NC}"
    echo -e "  • Enhanced JVM Metrics Dashboard"
    echo -e "  • Blockchain Sync Service Dashboard"
    echo -e "  • Ethereum Infrastructure Dashboard"
    echo -e "  • System Metrics Dashboard"
    echo ""
    echo -e "${GREEN}🔧 Key Metrics Available:${NC}"
    echo -e "  • JVM Memory Usage (Heap/Non-Heap)"
    echo -e "  • Garbage Collection Metrics"
    echo -e "  • Thread Metrics"
    echo -e "  • Class Loading Metrics"
    echo -e "  • HTTP Request Metrics"
    echo -e "  • Circuit Breaker States"
    echo -e "  • Custom Business Metrics"
    echo -e "  • Gateway Performance Metrics"
    echo ""
    echo -e "${YELLOW}💡 Tips:${NC}"
    echo -e "  • Metrics are collected every 15 seconds"
    echo -e "  • Dashboards auto-refresh every 5 seconds"
    echo -e "  • Check Prometheus targets at: http://localhost:9091/targets"
    echo -e "  • View raw metrics at: http://localhost:8080/actuator/prometheus"
}

# Main execution
main() {
    print_header "Enhanced Micrometer JVM Metrics Setup"
    
    check_docker
    build_api_gateway
    start_monitoring
    import_dashboard
    start_api_gateway
    verify_metrics
    show_access_info
    
    print_header "Setup Complete! 🎉"
    print_status "Your enhanced JVM metrics dashboard is now ready!"
}

# Run main function
main "$@"
