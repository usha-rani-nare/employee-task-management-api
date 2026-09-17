# --- build stage ---
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# download deps first so this layer gets cached and rebuilds are fast
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# --- run stage ---
# using a JRE (not full JDK) image here to keep the final image smaller
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# run as non-root - basic security practice, don't need root to run a jar
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=build /app/target/employee-task-manager.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser

EXPOSE 8080

# actuator health check - docker/k8s can use this to know if the container
# is actually ready to take traffic, not just "process is running"
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
