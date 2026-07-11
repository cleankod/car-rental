# syntax=docker/dockerfile:1

# The jar must already exist in build/libs (host/CI: ./gradlew bootJar) — this Dockerfile only
# packages it, it does not build it.

# Extract the boot jar into layers for better Docker layer caching.
FROM eclipse-temurin:25-jre AS extractor
WORKDIR /extractor
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM eclipse-temurin:25-jre AS runtime
WORKDIR /app

RUN addgroup --system app && adduser --system --ingroup app app
USER app

# Least-changing -> most-changing, so a code-only change reuses cached layers.
COPY --from=extractor /extractor/extracted/dependencies/ ./
COPY --from=extractor /extractor/extracted/spring-boot-loader/ ./
COPY --from=extractor /extractor/extracted/snapshot-dependencies/ ./
COPY --from=extractor /extractor/extracted/application/ ./

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
