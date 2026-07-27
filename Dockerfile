# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /build
COPY .mvn/settings.xml /tmp/settings.xml
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 mvn -s /tmp/settings.xml dependency:go-offline -B
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -s /tmp/settings.xml package -DskipTests -B

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
