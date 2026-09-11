# Multi-stage Docker build for SecureX Unified Production Deployment (Frontend + Backend)

# Stage 1: Build Vite Frontend
FROM node:20-alpine AS frontend-build
WORKDIR /app/frontend
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Spring Boot Backend with embedded static frontend
FROM maven:3.9.6-eclipse-temurin-17 AS backend-build
WORKDIR /app/backend
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B
COPY backend/src ./src
# Copy built Vite frontend static dist into Spring Boot static resources folder
COPY --from=frontend-build /app/frontend/dist ./src/main/resources/static
RUN mvn clean package -DskipTests

# Stage 3: Final Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S securex && adduser -S securex -G securex
RUN mkdir -p /app/uploads && chown -R securex:securex /app
COPY --from=backend-build /app/backend/target/securex-backend-1.0.0.jar app.jar
USER securex
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
