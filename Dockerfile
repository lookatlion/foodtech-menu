# Stage 1 — build the fat JAR using the Gradle wrapper
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy wrapper first so dependency resolution is cached as a separate layer.
# Re-runs only when build.gradle or settings.gradle change.
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
# Strip Windows CRLF line endings so the shell script + properties parse cleanly on Linux
RUN sed -i 's/\r$//' gradlew gradle/wrapper/gradle-wrapper.properties build.gradle settings.gradle \
    && chmod +x gradlew \
    && ./gradlew dependencies --no-daemon

# Copy source and build
COPY src src
RUN ./gradlew bootJar --no-daemon

# Stage 2 — minimal runtime image (no JDK, no Gradle)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
