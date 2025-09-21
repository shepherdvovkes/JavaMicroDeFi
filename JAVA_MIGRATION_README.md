# Java Migration from Java 8 to Java 17

This document describes the migration of the JavaMicroDeFi project from Java 8 to Java 17.

## Migration Summary

### What was changed:
1. **API Gateway**: Updated from Java 8 to Java 17
2. **Bitcoin Metrics Service**: Updated from Java 8 to Java 17  
3. **Data Aggregation Service**: Created with Java 17 (was incomplete before)
4. **All Dockerfiles**: Updated to use Java 17 base images
5. **Spring Boot versions**: Updated to 3.2.2 (requires Java 17+)
6. **Spring Cloud versions**: Updated to 2023.0.3

### Files Modified:
- `api-gateway/pom.xml` - Java version and Spring Boot version
- `bitcoin-metrics-service/pom.xml` - Java version and Spring Boot version
- `data-aggregation-service-java/pom.xml` - Created new file with Java 17
- `api-gateway/Dockerfile` - Updated base image to openjdk:17-jdk-alpine
- `bitcoin-metrics-service/Dockerfile` - Updated base image to openjdk:17-jre-alpine
- `data-aggregation-service-java/Dockerfile` - Created new Dockerfile

## Installing Java 17

### Option 1: Using SDKMAN (Recommended)
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 17
sdk install java 17.0.9-tem

# Set as default
sdk default java 17.0.9-tem
```

### Option 2: Using apt (Ubuntu/Debian)
```bash
# Update package list
sudo apt update

# Install OpenJDK 17
sudo apt install openjdk-17-jdk

# Set JAVA_HOME (add to ~/.bashrc)
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

### Option 3: Using Docker (for containerized builds)
```bash
# Use the updated Dockerfiles which now use Java 17
docker build -t api-gateway ./api-gateway
docker build -t bitcoin-metrics ./bitcoin-metrics-service
docker build -t data-aggregation ./data-aggregation-service-java
```

## Future Migration to Java 21

When Java 21 becomes more widely available, you can upgrade by:

1. **Installing Java 21**:
```bash
# Using SDKMAN
sdk install java 21.0.1-tem
sdk default java 21.0.1-tem
```

2. **Update pom.xml files**:
   - Change `<java.version>17</java.version>` to `<java.version>21</java.version>`
   - Change `<source>17</source>` and `<target>17</target>` to `<source>21</source>` and `<target>21</target>`

3. **Update Dockerfiles**:
   - Change `FROM openjdk:17-jdk-alpine` to `FROM openjdk:21-jdk-alpine`
   - Change `FROM openjdk:17-jre-alpine` to `FROM openjdk:21-jre-alpine`

## Benefits of the Migration

### From Java 8 to Java 17:
- **Performance**: Significant improvements in GC, startup time, and runtime performance
- **Security**: Latest security patches and improvements
- **Language Features**: Records, text blocks, pattern matching, sealed classes, etc.
- **Modern APIs**: Updated standard library with better HTTP client
- **Spring Boot 3.x**: Access to latest Spring features and improvements

### From Java 17 to Java 21 (future):
- **Virtual Threads**: Better concurrency and resource utilization
- **Pattern Matching**: Enhanced switch expressions and pattern matching
- **String Templates**: More readable string formatting
- **Foreign Function & Memory API**: Better native library integration

## Testing the Migration

After installing Java 17, test the migration:

```bash
# Test API Gateway
cd api-gateway
mvn clean compile

# Test Bitcoin Metrics Service  
cd ../bitcoin-metrics-service
mvn clean compile

# Test Data Aggregation Service
cd ../data-aggregation-service-java
mvn clean compile

# Build all services
cd ..
mvn clean package -f api-gateway/pom.xml
mvn clean package -f bitcoin-metrics-service/pom.xml
mvn clean package -f data-aggregation-service-java/pom.xml
```

## Branch Information

This migration is on the `java21` branch (named for the target version, though currently using Java 17 for compatibility).

To switch to this branch:
```bash
git checkout java21
```

To merge back to main:
```bash
git checkout main
git merge java21
```
