# syntax=docker/dockerfile:1

# ---- Build stage: full JDK + Maven wrapper ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src

# Resolve dependencies in their own layer so code changes don't re-download them.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

COPY src/ src/
RUN ./mvnw -B -q package -DskipTests && cp target/jobtracker-*.jar app.jar

# ---- Runtime stage: JRE only, non-root ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /src/app.jar app.jar
USER app

EXPOSE 8080
# Size the heap from the container's memory limit, not the host's.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75"
ENV SPRING_PROFILES_ACTIVE=postgres
ENTRYPOINT ["java", "-jar", "app.jar"]
