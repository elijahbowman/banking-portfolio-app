FROM maven:3.9.6-eclipse-temurin-17 AS build
#FROM openjdk:17-jdk-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests=true

FROM eclipse-temurin:17-jdk

WORKDIR /app

COPY javaagent/opentelemetry-javaagent.jar /app/opentelemetry-javaagent.jar

COPY --from=build /app/target/banking-service-0.0.1-SNAPSHOT.jar app.jar

RUN mkdir -p /app/logs && chmod 777 /app/logs

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

ENV OTEL_SERVICE_NAME=banking-service
ENV OTEL_TRACES_EXPORTER=otlp
ENV OTEL_METRICS_EXPORTER=otlp
ENV OTEL_LOGS_EXPORTER=otlp
ENV OTEL_PROPAGATORS=tracecontext,baggage
#ENV OTEL_LOGS_INCLUDE_MDC=true
ENV OTEL_EXPORTER_OTLP_ENDPOINT=http://host.docker.internal:4318

ENTRYPOINT ["java", "-javaagent:/app/opentelemetry-javaagent.jar", "-jar", "app.jar"]