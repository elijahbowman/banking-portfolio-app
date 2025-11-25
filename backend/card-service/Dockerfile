#FROM maven:3.9.6-eclipse-temurin-17 AS build
##FROM openjdk:17-jdk-slim AS build
#WORKDIR /app
#COPY pom.xml .
#COPY src ./src
#
#RUN mvn clean package -DskipTests=true
#
#FROM eclipse-temurin:17-jdk
#
#WORKDIR /app
#
#COPY --from=build /app/target/card-service-0.0.1-SNAPSHOT.jar app.jar
#
#RUN mkdir -p /app/logs && chmod 777 /app/logs
#
#EXPOSE 8082
#
#HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
#  CMD curl -f http://localhost:8082/actuator/health || exit 1
#
#ENTRYPOINT ["java", "-jar", "app.jar"]