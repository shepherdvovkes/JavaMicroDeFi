# SSD Cache Service Dockerfile
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    curl \
    wget \
    procps \
    iotop \
    iostat \
    && rm -rf /var/lib/apt/lists/*

# Copy application files
COPY target/ssd-cache-service-1.0.0.jar app.jar

# Create cache directories
RUN mkdir -p /opt/ssd-cache/memory \
    /opt/ssd-cache/ssd \
    /opt/ssd-cache/metrics \
    /var/log/ssd-cache

# Set permissions
RUN chmod 755 /opt/ssd-cache \
    && chmod 755 /opt/ssd-cache/memory \
    && chmod 755 /opt/ssd-cache/ssd \
    && chmod 755 /opt/ssd-cache/metrics \
    && chmod 755 /var/log/ssd-cache

# Create cache optimization script
RUN echo '#!/bin/bash\n\
echo "Optimizing SSD cache..."\n\
# Set I/O scheduler to none for NVMe\n\
for device in $(lsblk -d -o NAME,TYPE | grep disk | grep nvme | awk "{print \$1}"); do\n\
    echo "none" > /sys/block/$device/queue/scheduler 2>/dev/null || true\n\
    echo "1024" > /sys/block/$device/queue/nr_requests 2>/dev/null || true\n\
    echo "256" > /sys/block/$device/queue/read_ahead_kb 2>/dev/null || true\n\
    echo "write through" > /sys/block/$device/queue/write_cache 2>/dev/null || true\n\
    echo "performance" > /sys/block/$device/queue/power/control 2>/dev/null || true\n\
    echo "600" > /sys/block/$device/queue/io_timeout 2>/dev/null || true\n\
    echo "0" > /sys/block/$device/queue/nomerges 2>/dev/null || true\n\
    echo "1" > /sys/block/$device/queue/iostats 2>/dev/null || true\n\
done\n\
echo "SSD cache optimization completed"' > /usr/local/bin/optimize-ssd-cache.sh

RUN chmod +x /usr/local/bin/optimize-ssd-cache.sh

# Create cache monitoring script
RUN echo '#!/bin/bash\n\
echo "SSD Cache Performance Monitor"\n\
echo "=============================="\n\
echo "Date: $(date)"\n\
echo ""\n\
\n\
# Check SSD devices\n\
SSD_DEVICES=$(lsblk -d -o NAME,TYPE | grep disk | grep -E "(nvme|ssd)" | awk "{print \$1}")\n\
\n\
for device in $SSD_DEVICES; do\n\
    echo "Device: /dev/$device"\n\
    echo "  I/O Scheduler: $(cat /sys/block/$device/queue/scheduler 2>/dev/null || echo "N/A")"\n\
    echo "  Queue Depth: $(cat /sys/block/$device/queue/nr_requests 2>/dev/null || echo "N/A")"\n\
    echo "  Read Ahead: $(cat /sys/block/$device/queue/read_ahead_kb 2>/dev/null || echo "N/A") KB"\n\
    echo "  Write Cache: $(cat /sys/block/$device/queue/write_cache 2>/dev/null || echo "N/A")"\n\
    echo "  Power Control: $(cat /sys/block/$device/queue/power/control 2>/dev/null || echo "N/A")"\n\
    echo "  I/O Timeout: $(cat /sys/block/$device/queue/io_timeout 2>/dev/null || echo "N/A") seconds"\n\
    echo "  No Merges: $(cat /sys/block/$device/queue/nomerges 2>/dev/null || echo "N/A")"\n\
    echo "  I/O Stats: $(cat /sys/block/$device/queue/iostats 2>/dev/null || echo "N/A")"\n\
    echo ""\n\
done\n\
\n\
# Show I/O statistics\n\
echo "I/O Statistics (last 5 seconds):"\n\
iostat -x 1 1 2>/dev/null | grep -E "(Device|nvme|ssd)" || echo "iostat not available"' > /usr/local/bin/monitor-ssd-cache.sh

RUN chmod +x /usr/local/bin/monitor-ssd-cache.sh

# Create cache performance test script
RUN echo '#!/bin/bash\n\
echo "SSD Cache Performance Test"\n\
echo "=========================="\n\
\n\
CACHE_DIR="/opt/ssd-cache/ssd"\n\
TEST_FILE="$CACHE_DIR/performance_test.dat"\n\
TEST_SIZE="1G"\n\
\n\
echo "Creating test file of size $TEST_SIZE..."\n\
dd if=/dev/zero of="$TEST_FILE" bs=1M count=1024 2>/dev/null\n\
\n\
echo "Testing read performance..."\n\
READ_START=$(date +%s.%N)\n\
dd if="$TEST_FILE" of=/dev/null bs=1M 2>/dev/null\n\
READ_END=$(date +%s.%N)\n\
READ_TIME=$(echo "$READ_END - $READ_START" | bc 2>/dev/null || echo "0")\n\
READ_SPEED=$(echo "scale=2; 1024 / $READ_TIME" | bc 2>/dev/null || echo "0")\n\
echo "Read Speed: ${READ_SPEED} MB/s"\n\
\n\
echo "Testing write performance..."\n\
WRITE_START=$(date +%s.%N)\n\
dd if=/dev/zero of="$TEST_FILE" bs=1M count=1024 2>/dev/null\n\
WRITE_END=$(date +%s.%N)\n\
WRITE_TIME=$(echo "$WRITE_END - $WRITE_START" | bc 2>/dev/null || echo "0")\n\
WRITE_SPEED=$(echo "scale=2; 1024 / $WRITE_TIME" | bc 2>/dev/null || echo "0")\n\
echo "Write Speed: ${WRITE_SPEED} MB/s"\n\
\n\
# Cleanup\n\
rm -f "$TEST_FILE"\n\
echo "Test completed and cleaned up."' > /usr/local/bin/test-ssd-cache.sh

RUN chmod +x /usr/local/bin/test-ssd-cache.sh

# Install bc for calculations
RUN apt-get update && apt-get install -y bc && rm -rf /var/lib/apt/lists/*

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -Xms1g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV CACHE_SSD_ENABLED=true
ENV CACHE_SSD_SIZE=1073741824
ENV CACHE_MEMORY_SIZE=134217728
ENV CACHE_TTL_HOT=60
ENV CACHE_TTL_WARM=3600
ENV CACHE_TTL_COLD=86400

# Expose port
EXPOSE 8088

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8088/api/cache/health || exit 1

# Run optimization script on startup
RUN echo '#!/bin/bash\n\
/usr/local/bin/optimize-ssd-cache.sh\n\
exec java $JAVA_OPTS -jar app.jar' > /usr/local/bin/start-cache-service.sh

RUN chmod +x /usr/local/bin/start-cache-service.sh

# Use the startup script
CMD ["/usr/local/bin/start-cache-service.sh"]

