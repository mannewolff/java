FROM node:20 AS frontend-build
WORKDIR /frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
# Optional build-arg overrides for local Docker builds (via docker-compose.override.yml).
# When not passed, the ARG is empty and NOT exported — Vite then reads .env.production
# automatically (production Keycloak). When passed (local dev), they override .env.production.
ARG VITE_KEYCLOAK_URL
ARG VITE_KEYCLOAK_REALM
ARG VITE_KEYCLOAK_CLIENT_ID
RUN if [ -n "${VITE_KEYCLOAK_URL}" ];     then export VITE_KEYCLOAK_URL="${VITE_KEYCLOAK_URL}"; fi \
 && if [ -n "${VITE_KEYCLOAK_REALM}" ];   then export VITE_KEYCLOAK_REALM="${VITE_KEYCLOAK_REALM}"; fi \
 && if [ -n "${VITE_KEYCLOAK_CLIENT_ID}" ]; then export VITE_KEYCLOAK_CLIENT_ID="${VITE_KEYCLOAK_CLIENT_ID}"; fi \
 && npm run build

FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline -P skip-frontend
COPY src ./src
COPY --from=frontend-build /frontend/dist /workspace/src/main/resources/static
RUN mvn -B -DskipTests -P skip-frontend package

FROM eclipse-temurin:25-jre
WORKDIR /app

# Install curl for the healthcheck. Done as root, before dropping privileges.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Non-root runtime user (CLAUDE-security.md / code review P2-9).
# A breakout from the JVM stops at an unprivileged account; the container
# cannot write outside /app and cannot run privileged operations.
RUN groupadd --system --gid 1001 spring \
    && useradd  --system --uid 1001 --gid spring --no-create-home --shell /usr/sbin/nologin spring

COPY --from=build --chown=spring:spring /workspace/target/*.jar /app/app.jar
RUN chown -R spring:spring /app

USER spring:spring
EXPOSE 8080
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
