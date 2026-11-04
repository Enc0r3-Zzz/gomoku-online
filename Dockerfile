FROM eclipse-temurin:17-jre

WORKDIR /app

RUN useradd -r -u 1001 appuser

COPY target/gomoku-*.jar app.jar

USER appuser

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "/app/app.jar"]
