FROM gradle:8.4-jdk17 AS build

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

RUN gradle build --no-daemon -x test

FROM openjdk:17-jre-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

RUN useradd -r -s /bin/false appuser && chown appuser:appuser app.jar
USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]