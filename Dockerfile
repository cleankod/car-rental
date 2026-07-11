# syntax=docker/dockerfile:1

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace

COPY gradlew ./
COPY gradle gradle
COPY settings.gradle build.gradle ./
RUN chmod +x gradlew

COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
