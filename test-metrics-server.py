#!/usr/bin/env python3
"""
Simple test metrics server to verify Prometheus scraping
"""
import time
import random
from http.server import HTTPServer, BaseHTTPRequestHandler
from prometheus_client import Counter, Histogram, Gauge, generate_latest, CONTENT_TYPE_LATEST

# Create metrics
request_count = Counter('http_requests_total', 'Total HTTP requests', ['method', 'endpoint'])
request_duration = Histogram('http_request_duration_seconds', 'HTTP request duration')
memory_usage = Gauge('memory_usage_bytes', 'Memory usage in bytes')
cpu_usage = Gauge('cpu_usage_percent', 'CPU usage percentage')
active_connections = Gauge('active_connections', 'Number of active connections')

class MetricsHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/metrics':
            # Update some metrics with random values
            memory_usage.set(random.randint(100000000, 500000000))  # 100MB - 500MB
            cpu_usage.set(random.uniform(10, 80))  # 10% - 80%
            active_connections.set(random.randint(5, 50))
            
            # Record this request
            request_count.labels(method='GET', endpoint='/metrics').inc()
            
            self.send_response(200)
            self.send_header('Content-Type', CONTENT_TYPE_LATEST)
            self.end_headers()
            self.wfile.write(generate_latest())
        else:
            self.send_response(404)
            self.end_headers()
    
    def log_message(self, format, *args):
        # Suppress default logging
        pass

if __name__ == '__main__':
    server = HTTPServer(('0.0.0.0', 8080), MetricsHandler)
    print("Test metrics server running on http://0.0.0.0:8080/metrics")
    server.serve_forever()
