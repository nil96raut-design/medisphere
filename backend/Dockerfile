# ---- Build stage ----
FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Layer 1: Maven wrapper + pom.xml (cached until deps change)
COPY mvnw pom.xml ./
COPY .mvn .mvn/
RUN ./mvnw dependency:go-offline -B -q

# Layer 2: Source code (invalidates only when src changes)
COPY src src/

# Build fat JAR, skip tests for speed
RUN ./mvnw clean package -DskipTests -B -q

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy only the JAR from builder
COPY --from=builder /app/target/*.jar app.jar

# Secure permissions
RUN chown -R appuser:appgroup /app

USER appuser

# Render provides PORT env var; Spring reads it from environment
ENV PORT=8085

EXPOSE 8085

ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx384m", \
  "-XX:+UseSerialGC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
