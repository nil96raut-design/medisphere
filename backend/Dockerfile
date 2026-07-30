# ==============================================================================
# STAGE 1: Builder Stage (Maven Dependencies & Package Build)
# ==============================================================================
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

# Container Metadata (Builder)
LABEL maintainer="MediSphere DevOps Team <devops@medisphere.com>"
LABEL service.name="medisphere-backend-builder"

# Copy Maven wrapper & pom.xml first to maximize layer caching
COPY mvnw pom.xml ./
COPY .mvn .mvn/

# Ensure wrapper script is executable and resolve dependencies offline
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source code and build production package
COPY src src/
RUN ./mvnw clean package -DskipTests -B

# ==============================================================================
# STAGE 2: Production Runtime Stage (Minimal Alpine JRE Container)
# ==============================================================================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Container Metadata (Runtime)
LABEL maintainer="MediSphere DevOps Team <devops@medisphere.com>"
LABEL service.name="medisphere-backend"

# Environment Defaults for Render Deployment
ENV PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod

# Create secure non-root system group and user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy built JAR artifact from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Enforce secure ownership and switch user context
RUN chown -R appuser:appgroup /app
USER appuser

# Expose target application port
EXPOSE 8080

# Production Health Check Probe (/api/admin/health)
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:${PORT}/api/admin/health || exit 1

# Optimized JVM Execution Entrypoint respecting Render dynamic $PORT
ENTRYPOINT ["sh", "-c", "exec java \
  -Xms128m \
  -Xmx256m \
  -XX:MaxMetaspaceSize=128m \
  -XX:+UseContainerSupport \
  -XX:+UseSerialGC \
  -XX:+ExitOnOutOfMemoryError \
  -Dserver.port=${PORT} \
  -jar app.jar"]
