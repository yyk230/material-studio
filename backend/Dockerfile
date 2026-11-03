FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
COPY mvnw pom.xml ./
COPY .mvn .mvn
# 从 Windows 构建时可执行位不一定保留
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests && cp target/studio-backend.jar /app.jar

FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app app \
    && mkdir -p /data/storage && chown -R app:app /data
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
ENV APP_STORAGE_ROOT=/data/storage
EXPOSE 8080
ENTRYPOINT ["java", "-Duser.timezone=UTC", "-jar", "/app/app.jar"]
