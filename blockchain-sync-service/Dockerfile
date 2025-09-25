# Multi-stage build for optimized Docker image
FROM rust:1.90-slim as builder

# Install system dependencies
RUN apt-get update && apt-get install -y \
    pkg-config \
    libssl-dev \
    libclang-dev \
    cmake \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /usr/src/app

# Copy manifest files
COPY Cargo.toml Cargo.lock ./

# Create a dummy main.rs to build dependencies
RUN mkdir src && echo "fn main() {}" > src/main.rs

# Build dependencies (this layer will be cached)
RUN cargo build --release

# Remove dummy main.rs and copy actual source code
RUN rm src/main.rs
COPY src ./src

# Build the actual application
RUN cargo build --release

# Runtime stage
FROM debian:bookworm-slim

# Install runtime dependencies
RUN apt-get update && apt-get install -y \
    ca-certificates \
    libssl3 \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN useradd -r -s /bin/false blockchain-sync

# Set working directory
WORKDIR /app

# Copy binary from builder stage
COPY --from=builder /usr/src/app/target/release/blockchain-sync-service /app/

# Change ownership to non-root user
RUN chown blockchain-sync:blockchain-sync /app/blockchain-sync-service

# Switch to non-root user
USER blockchain-sync

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD ./blockchain-sync-service status || exit 1

# Expose metrics port
EXPOSE 9090

# Default command - run sync with default parameters
CMD ["./blockchain-sync-service", "sync", "--max-blocks", "0", "--block-delay-ms", "1000"]
