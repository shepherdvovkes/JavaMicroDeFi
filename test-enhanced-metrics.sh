#!/bin/bash

# Test Enhanced Micrometer JVM Metrics
# This script tests the enhanced metrics system

set -e

# Colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

print_status() {
    echo -e "${GREEN}[TEST]${NC} $1"
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

# Test API Gateway metrics endpoint
test_metrics_endpoint() {
    print_header "Testing Metrics Endpoint"
    
    print_status "Testing API Gateway metrics endpoint..."
    
    if curl -s http://localhost:8080/actuator/prometheus > /dev/null; then
        print_status "✓ Metrics endpoint is accessible"
        
        # Check for specific metrics
        metrics=$(curl -s http://localhost:8080/actuator/prometheus)
        
        if echo "$metrics" | grep -q "jvm_memory_used_bytes"; then
            print_status "✓ JVM memory metrics found"
        else
            print_warning "⚠ JVM memory metrics not found"
        fi
        
        if echo "$metrics" | grep -q "http_server_requests"; then
            print_status "✓ HTTP server metrics found"
        else
            print_warning "⚠ HTTP server metrics not found"
        fi
        
        if echo "$metrics" | grep -q "gateway_requests_total"; then
            print_status "✓ Gateway metrics found"
        else
            print_warning "⚠ Gateway metrics not found"
        fi
        
        if echo "$metrics" | grep -q "business_"; then
            print_status "✓ Business metrics found"
        else
            print_warning "⚠ Business metrics not found"
        fi
        
    else
        print_error "✗ Metrics endpoint not accessible"
        return 1
    fi
}

# Test Prometheus targets
test_prometheus_targets() {
    print_header "Testing Prometheus Targets"
    
    print_status "Checking Prometheus targets..."
    
    if curl -s http://localhost:9091/api/v1/targets > /dev/null; then
        print_status "✓ Prometheus API is accessible"
        
        targets=$(curl -s http://localhost:9091/api/v1/targets)
        
        if echo "$targets" | grep -q "api-gateway"; then
            print_status "✓ API Gateway target found in Prometheus"
        else
            print_warning "⚠ API Gateway target not found in Prometheus"
        fi
        
    else
        print_error "✗ Prometheus API not accessible"
        return 1
    fi
}

# Test Grafana dashboard
test_grafana_dashboard() {
    print_header "Testing Grafana Dashboard"
    
    print_status "Checking Grafana health..."
    
    if curl -s http://localhost:3000/api/health > /dev/null; then
        print_status "✓ Grafana is accessible"
        
        # Check if dashboard exists
        dashboards=$(curl -s -u admin:defimon123 http://localhost:3000/api/search?type=dash-db)
        
        if echo "$dashboards" | grep -q "Enhanced JVM Metrics"; then
            print_status "✓ Enhanced JVM Metrics Dashboard found"
        else
            print_warning "⚠ Enhanced JVM Metrics Dashboard not found"
        fi
        
    else
        print_error "✗ Grafana not accessible"
        return 1
    fi
}

# Test specific metrics queries
test_metrics_queries() {
    print_header "Testing Metrics Queries"
    
    print_status "Testing JVM memory query..."
    if curl -s "http://localhost:9091/api/v1/query?query=jvm_memory_used_bytes" | grep -q "result"; then
        print_status "✓ JVM memory metrics query successful"
    else
        print_warning "⚠ JVM memory metrics query failed"
    fi
    
    print_status "Testing HTTP requests query..."
    if curl -s "http://localhost:9091/api/v1/query?query=http_server_requests_seconds_count" | grep -q "result"; then
        print_status "✓ HTTP requests metrics query successful"
    else
        print_warning "⚠ HTTP requests metrics query failed"
    fi
    
    print_status "Testing Gateway requests query..."
    if curl -s "http://localhost:9091/api/v1/query?query=gateway_requests_total" | grep -q "result"; then
        print_status "✓ Gateway requests metrics query successful"
    else
        print_warning "⚠ Gateway requests metrics query failed"
    fi
}

# Generate some test load
generate_test_load() {
    print_header "Generating Test Load"
    
    print_status "Generating test requests to create metrics data..."
    
    # Generate some HTTP requests
    for i in {1..10}; do
        curl -s http://localhost:8080/actuator/health > /dev/null &
        curl -s http://localhost:8080/actuator/info > /dev/null &
    done
    
    wait
    print_status "✓ Test load generated"
    
    # Wait for metrics to be collected
    print_status "Waiting for metrics to be collected..."
    sleep 30
}

# Display test summary
show_test_summary() {
    print_header "Test Summary"
    
    echo -e "${GREEN}🎯 Test Results:${NC}"
    echo -e "  • Metrics Endpoint: $(curl -s http://localhost:8080/actuator/prometheus > /dev/null && echo "✓ PASS" || echo "✗ FAIL")"
    echo -e "  • Prometheus API: $(curl -s http://localhost:9091/api/v1/targets > /dev/null && echo "✓ PASS" || echo "✗ FAIL")"
    echo -e "  • Grafana API: $(curl -s http://localhost:3000/api/health > /dev/null && echo "✓ PASS" || echo "✗ FAIL")"
    echo ""
    echo -e "${GREEN}📊 Available Metrics:${NC}"
    echo -e "  • JVM Metrics: $(curl -s http://localhost:8080/actuator/prometheus | grep -c "jvm_" || echo "0")"
    echo -e "  • HTTP Metrics: $(curl -s http://localhost:8080/actuator/prometheus | grep -c "http_" || echo "0")"
    echo -e "  • Gateway Metrics: $(curl -s http://localhost:8080/actuator/prometheus | grep -c "gateway_" || echo "0")"
    echo -e "  • Business Metrics: $(curl -s http://localhost:8080/actuator/prometheus | grep -c "business_" || echo "0")"
    echo ""
    echo -e "${GREEN}🔗 Access URLs:${NC}"
    echo -e "  • Grafana Dashboard: http://localhost:3000"
    echo -e "  • Prometheus: http://localhost:9091"
    echo -e "  • Metrics Endpoint: http://localhost:8080/actuator/prometheus"
}

# Main test execution
main() {
    print_header "Enhanced Metrics System Test"
    
    test_metrics_endpoint
    test_prometheus_targets
    test_grafana_dashboard
    generate_test_load
    test_metrics_queries
    show_test_summary
    
    print_header "Test Complete! 🎉"
}

# Run main function
main "$@"
