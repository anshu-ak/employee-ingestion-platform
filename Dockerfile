FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY . .

RUN chmod +x mvnw \
    && ./mvnw clean package -DskipTests


FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system employee \
    && useradd --system \
        --gid employee \
        --create-home \
        employee

COPY --from=build \
    /workspace/target/employee-ingestion-platform-0.0.1-SNAPSHOT.jar \
    app.jar

RUN mkdir -p /app/data/uploads \
    && chown -R employee:employee /app

USER employee

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]