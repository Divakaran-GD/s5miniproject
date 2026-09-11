# Multi-stage Docker build for SecureX Backend
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S securex && adduser -S securex -G securex
RUN mkdir -p /app/uploads && chown -R securex:securex /app
COPY --from=build /app/target/securex-backend-1.0.0.jar app.jar
USER securex
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
