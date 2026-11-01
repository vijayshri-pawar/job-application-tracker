# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Download dependencies first (cached between builds while pom.xml is unchanged)
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in GitHub Actions (see .github/workflows/ci.yml), so they are skipped here
RUN mvn -B -q -DskipTests package

# ---------- Stage 2: small image that only runs the app ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a normal user, not root
RUN useradd --system --uid 1001 appuser
COPY --from=build /app/target/*.jar app.jar
USER appuser

# Render (and most hosts) pass the port in the PORT variable; application.properties reads it
EXPOSE 8080
# Use at most 75% of the container's memory (free plans have little RAM)
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
