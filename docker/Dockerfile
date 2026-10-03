# syntax=docker/dockerfile:1

# ---- Stage 1: build the jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
# Files checked out on Windows can contain extra CR characters, which break scripts on Linux.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
COPY src/ src/
# The cache mount keeps downloaded libraries between builds, so rebuilds are fast.
RUN --mount=type=cache,target=/root/.m2 ./mvnw -B -Dmaven.test.skip=true package

# ---- Stage 2: small runtime image (JRE only, no build tools) ----
FROM eclipse-temurin:21-jre
WORKDIR /app
# Run as a normal user instead of root, so a bug in the app cannot control the container.
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /workspace/target/ecommerce-backend-*.jar app.jar
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]