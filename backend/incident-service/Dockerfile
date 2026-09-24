# syntax=docker/dockerfile:1

# Stage 1: Build
FROM maven:3.9-eclipse-temurin-21 AS builder

# Sources des modules partagés, installés dans le même step que la résolution.
WORKDIR /build/common-security
COPY common-security/pom.xml .
COPY common-security/src ./src

WORKDIR /build/common-contracts
COPY common-contracts/pom.xml .
COPY common-contracts/src ./src

# Installer common-security PUIS résoudre les deps dans le MÊME RUN : l'install ne peut
# plus être sautée (cache de couche) pendant que la résolution se ré-exécute sur un
# cache-mount Maven vidé (sinon "Could not find artifact common-security").
WORKDIR /build/incident-service
COPY incident-service/pom.xml .
RUN --mount=type=cache,id=fintrack-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -q -f /build/common-security/pom.xml install -DskipTests && \
    mvn -B -q -f /build/common-contracts/pom.xml install -DskipTests && \
    mvn -B dependency:resolve

COPY incident-service/src ./src
RUN --mount=type=cache,id=fintrack-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -q -f /build/common-security/pom.xml install -DskipTests && \
    mvn -B -q -f /build/common-contracts/pom.xml install -DskipTests && \
    mvn -B package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
RUN addgroup -S fintrack && adduser -S fintrack -G fintrack
COPY --from=builder /build/incident-service/target/*.jar app.jar
USER fintrack

EXPOSE 8082

HEALTHCHECK --interval=15s --timeout=10s --start-period=90s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8082/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
