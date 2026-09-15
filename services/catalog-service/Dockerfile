# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /workspace

COPY mvnw pom.xml ./
COPY .mvn .mvn
COPY src src
RUN chmod +x mvnw && ./mvnw -B -DskipTests package \
    && cp target/catalog-service-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app

RUN useradd --system --create-home --uid 10001 appuser
USER appuser

COPY --from=build /workspace/app.jar /app/app.jar

ENV SERVER_PORT=5003
EXPOSE 5003

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
