#!/bin/bash

# Linea Container Group Deployment Script
# This script deploys the complete Linea microservice stack to localhost

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
COMPOSE_FILE="docker-compose-linea-group.yml"
PROJECT_NAME="linea"
DATA_PATH="/mnt/sata18tb"
LOG_FILE="deployment.log"

# Functions
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
}

check_prerequisites() {
    log_message "Checking prerequisites..."
    
    # Check Docker
    if ! command -v docker &> /dev/null; then
        log_error "Docker is not installed. Please install Docker first."
        exit 1
    fi
    
    # Check Docker Compose
    if ! command -v docker-compose &> /dev/null; then
        log_error "Docker Compose is not installed. Please install Docker Compose first."
        exit 1
    fi
    
    # Check if Docker daemon is running
    if ! docker info &> /dev/null; then
        log_error "Docker daemon is not running. Please start Docker first."
        exit 1
    fi
    
    # Check data directory
    if [ ! -d "$DATA_PATH" ]; then
        log_warning "Data directory $DATA_PATH does not exist. Creating it..."
        sudo mkdir -p "$DATA_PATH"
        sudo chown -R $USER:$USER "$DATA_PATH"
    fi
    
    # Check available disk space
    AVAILABLE_SPACE=$(df "$DATA_PATH" | awk 'NR==2 {print $4}')
    if [ "$AVAILABLE_SPACE" -lt 50000000 ]; then  # 50GB in KB
        log_warning "Available disk space is low. At least 50GB is recommended."
    fi
    
    log_success "Prerequisites check completed"
}

cleanup_previous_deployment() {
    log_message "Cleaning up previous deployment..."
    
    # Stop and remove existing containers
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" down --volumes --remove-orphans 2>/dev/null || true
    
    # Remove unused images
    docker image prune -f
    
    log_success "Cleanup completed"
}

build_images() {
    log_message "Building Docker images..."
    
    # Build the Linea microservice image
    log_message "Building Linea microservice image..."
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" build --no-cache linea-microservice
    
    log_success "Images built successfully"
}

start_services() {
    log_message "Starting Linea container group..."
    
    # Start all services
    docker-compose -f "$COMPOSE_FILE" -p "$PROJECT_NAME" up -d
    
    log_success "Services started successfully"
}

wait_for_services() {
    log_message "Waiting for services to be ready..."
    
    # Wait for databases
    log_message "Waiting for MongoDB..."
    timeout 60 bash -c 'until docker exec linea-mongodb mongosh --eval "db.runCommand({ping: 1})" >/dev/null 2>&1; do sleep 2; done'
    
    log_message "Waiting for TimescaleDB..."
    timeout 60 bash -c 'until docker exec linea-timescaledb pg_isready -U linea_user -d linea_metrics >/dev/null 2>&1; do sleep 2; done'
    
    log_message "Waiting for Redis..."
    timeout 60 bash -c 'until docker exec linea-redis redis-cli ping >/dev/null 2>&1; do sleep 2; done'
    
    log_message "Waiting for MySQL..."
    timeout 60 bash -c 'until docker exec linea-mysql mysqladmin ping -h localhost >/dev/null 2>&1; do sleep 2; done'
    
    # Wait for Linea microservice
    log_message "Waiting for Linea microservice..."
    timeout 120 bash -c 'until curl -f http://localhost:8008/api/actuator/health >/dev/null 2>&1; do sleep 5; done'
    
    # Wait for monitoring services
    log_message "Waiting for Prometheus..."
    timeout 60 bash -c 'until curl -f http://localhost:9090/-/healthy >/dev/null 2>&1; do sleep 2; done'
    
    log_message "Waiting for Grafana..."
    timeout 60 bash -c 'until curl -f http://localhost:3000/api/health >/dev/null 2>&1; do sleep 2; done'
    
    log_success "All services are ready"
}

show_access_info() {
    log_message "Deployment completed successfully!"
    echo ""
    echo "🚀 Linea Container Group is now running on localhost"
    echo ""
    echo "📊 Access Points:"
    echo "  • Linea Microservice:     http://localhost:8008"
    echo "  • Prometheus:             http://localhost:9090"
    echo "  • Grafana:                http://localhost:3000 (admin/admin)"
    echo "  • MongoDB Express:        http://localhost:8081 (admin/admin)"
    echo "  • pgAdmin:                http://localhost:8082 (admin@linea.local/admin)"
    echo "  • Redis Commander:        http://localhost:8083"
    echo "  • Node Exporter:          http://localhost:9100"
    echo "  • cAdvisor:               http://localhost:8080"
    echo ""
    echo "🗄️  Database Connections:"
    echo "  • MongoDB:                localhost:27017"
    echo "  • TimescaleDB:            localhost:5432"
    echo "  • Redis:                  localhost:6379"
    echo "  • MySQL:                  localhost:3306"
    echo ""
    echo "📁 Data Storage:"
    echo "  • Blockchain Data:       $DATA_PATH"
    echo "  • MongoDB Data:          Docker volume"
    echo "  • TimescaleDB Data:      Docker volume"
    echo "  • Redis Data:            Docker volume"
    echo "  • MySQL Data:            Docker volume"
    echo ""
    echo "🔧 Management Commands:"
    echo "  • View logs:             docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME logs -f"
    echo "  • Stop services:         docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME down"
    echo "  • Restart services:      docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME restart"
    echo "  • View status:           docker-compose -f $COMPOSE_FILE -p $PROJECT_NAME ps"
    echo ""
    echo "📈 Monitoring:"
    echo "  • Check service health:  curl http://localhost:8008/api/actuator/health"
    echo "  • View metrics:          curl http://localhost:8008/api/actuator/prometheus"
    echo "  • Prometheus targets:    http://localhost:9090/targets"
    echo "  • Grafana dashboards:    http://localhost:3000/dashboards"
    echo ""
}

start_synchronization() {
    log_message "Starting Linea node synchronization..."
    
    # Wait a bit for the service to fully initialize
    sleep 30
    
    # Start the synchronization process
    log_message "Triggering Linea blockchain synchronization..."
    
    # Call the sync endpoint
    curl -X POST http://localhost:8008/api/linea/sync/start \
        -H "Content-Type: application/json" \
        -d '{"syncType": "realtime", "startBlock": 0}' \
        || log_warning "Could not trigger sync via API (service might still be starting)"
    
    log_success "Synchronization process initiated"
    log_message "The Linea microservice will now start collecting blockchain data from the 35GB dataset in $DATA_PATH"
}

# Main execution
main() {
    echo "🚀 Linea Container Group Deployment"
    echo "=================================="
    echo ""
    
    # Remove old log file
    rm -f "$LOG_FILE"
    
    # Execute deployment steps
    check_prerequisites
    cleanup_previous_deployment
    build_images
    start_services
    wait_for_services
    start_synchronization
    show_access_info
    
    log_success "Linea Container Group deployment completed successfully!"
    log_message "Check $LOG_FILE for detailed logs"
}

# Run main function
main "$@"
