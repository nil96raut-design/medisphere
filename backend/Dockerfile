# ---------- Stage 1: Build ----------
FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY mvnw pom.xml ./
COPY .mvn .mvn/
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src/
RUN ./mvnw clean package -DskipTests -B


# ---------- Stage 2: Runtime ----------
FROM eclipse-temurin:17-jdk-jammy
WORKDIR /app

# Create non-root user
RUN addgroup --system appgroup && adduser --system appuser --ingroup appgroup

COPY --from=builder /app/target/*.jar app.jar
RUN chown -R appuser:appgroup /app

USER appuser

# DO NOT hardcode PORT
# Spring Boot will use ${PORT}

ENTRYPOINT ["java","-jar","app.jar"]