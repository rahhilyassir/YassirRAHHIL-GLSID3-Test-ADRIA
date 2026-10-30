# syntax=docker/dockerfile:1

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

ARG MODULE

COPY pom.xml ./
COPY . .

RUN mvn -pl ${MODULE} -am -DskipTests clean package spring-boot:repackage

FROM eclipse-temurin:17-jre
WORKDIR /app

ARG MODULE
ARG PORT=8080

ENV PORT=${PORT}

RUN apt-get update && apt-get install -y --no-install-recommends wget && rm -rf /var/lib/apt/lists/* && \
    groupadd --system spring && \
    useradd --system --gid spring --create-home --home-dir /home/spring spring && \
    chown -R spring:spring /app

COPY --from=build /workspace/${MODULE}/target/*.jar /app/app.jar

USER spring:spring

EXPOSE ${PORT}

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=5 \
  CMD wget -qO- "http://localhost:${PORT}/actuator/health" | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["sh", "-c", "java -jar /app/app.jar --server.port=${PORT}"]
