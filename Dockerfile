# Multi-stage build (deployment enhancement):
#   build   - full JDK + Maven, compiles and packages the fat JAR
#   runtime - JRE only on Alpine; Maven, the JDK and the source code never reach it
#
#   docker build -t password-generator-api:local .
#   docker run --rm -p 8081:8081 password-generator-api:local

# ---------------------------------------------------------------- Stage 1: build
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Dependencies first, in their own layer: they are re-downloaded only when pom.xml changes,
# not on every source edit.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Jenkins passes b<build number>-<commit SHA>; it becomes the JAR name and the app version.
ARG BUILD_TAG=docker
RUN mvn -B package -DskipTests -Dbuild.tag="${BUILD_TAG}" \
 && cp "target/password-generator-api-${BUILD_TAG}.jar" /app.jar

# -------------------------------------------------------------- Stage 2: runtime
FROM eclipse-temurin:21-jre-alpine AS runtime

ARG BUILD_TAG=docker
ARG VCS_REF=unknown
LABEL org.opencontainers.image.title="password-generator-api" \
      org.opencontainers.image.description="Password Generator API (Spring Boot)" \
      org.opencontainers.image.version="${BUILD_TAG}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.source="https://github.com/tonne-omkar/password-generator-devops"

# Run as an unprivileged user, not root.
RUN addgroup -S app && adduser -S -G app -u 10001 app
WORKDIR /app
COPY --from=build /app.jar app.jar
USER 10001

# Let the JVM size its heap from the container memory limit, not the host's RAM.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75" \
    APP_VERSION="${BUILD_TAG}"
EXPOSE 8081

HEALTHCHECK --interval=15s --timeout=3s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8081/actuator/health >/dev/null || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
