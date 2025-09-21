FROM python:3.11-slim

WORKDIR /app

# Install prometheus_client
RUN pip install prometheus_client

# Copy the test metrics server
COPY test-metrics-server.py .

# Expose port
EXPOSE 8080

# Run the server
CMD ["python", "test-metrics-server.py"]
