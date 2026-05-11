# ── Stage 1: extract layered jar ─────────────────────────────────────────────
FROM eclipse-temurin:21-jre AS builder
WORKDIR /application
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} application.jar
RUN java -Djarmode=layertools -jar application.jar extract

# ── Stage 2: production image ─────────────────────────────────────────────────
FROM eclipse-temurin:21-jre
WORKDIR /application

# Non-root user for security
RUN addgroup --system appgroup && adduser --system --ingroup appgroup appuser

# Create data directory for H2 file storage
RUN mkdir -p /application/data && chown appuser:appgroup /application/data

COPY --from=builder /application/dependencies/ ./
COPY --from=builder /application/spring-boot-loader/ ./
COPY --from=builder /application/snapshot-dependencies/ ./
COPY --from=builder /application/application/ ./

USER appuser

ENV SPRING_PROFILES_ACTIVE=prod

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
