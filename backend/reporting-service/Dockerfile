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
WORKDIR /build/reporting-service
COPY reporting-service/pom.xml .
RUN --mount=type=cache,id=fintrack-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -q -f /build/common-security/pom.xml install -DskipTests && \
    mvn -B -q -f /build/common-contracts/pom.xml install -DskipTests && \
    mvn -B dependency:resolve -Dhttps.protocols=TLSv1.2,TLSv1.3 -Djdk.tls.client.protocols=TLSv1.2,TLSv1.3 -Dmaven.wagon.http.retryHandler.count=3

COPY reporting-service/src ./src
RUN --mount=type=cache,id=fintrack-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -q -f /build/common-security/pom.xml install -DskipTests && \
    mvn -B -q -f /build/common-contracts/pom.xml install -DskipTests && \
    mvn -B package -DskipTests

# Stage 2: Extract Spring Boot layers
FROM eclipse-temurin:21-jre-alpine AS layers

WORKDIR /app
COPY --from=builder /build/reporting-service/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher --destination /app --force

# Stage 3: Runtime
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Outils de sauvegarde : le dump se fait par le reseau (postgresql:5434, mongodb:27019)
# puis rclone synchronise vers Backblaze B2.
RUN apk add --no-cache postgresql17-client mongodb-tools rclone

RUN addgroup -S fintrack && adduser -S fintrack -G fintrack \
    && mkdir -p /data/fintrack/backups /home/fintrack/.config/rclone \
    && chown -R fintrack:fintrack /data/fintrack/backups /home/fintrack
COPY --from=layers /app/dependencies/ ./
COPY --from=layers /app/spring-boot-loader/ ./
COPY --from=layers /app/snapshot-dependencies/ ./
COPY --from=layers /app/application/ ./
USER fintrack

EXPOSE 8085

HEALTHCHECK --interval=15s --timeout=10s --start-period=90s --retries=5 \
    CMD wget --quiet --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
