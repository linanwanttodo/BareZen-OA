FROM eclipse-temurin:21-jre-alpine AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew ./
COPY gradle gradle
COPY bz_framework_core bz_framework_core
COPY bz_web_app bz_web_app

RUN chmod +x gradlew && ./gradlew :bz_web_app:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

COPY --from=builder /app/bz_web_app/build/libs/*.jar app.jar
COPY --from=builder /app/bz_framework_core/build/libs/*.jar libs/

RUN apk add --no-cache curl

EXPOSE 8080

ENV TZ=Asia/Shanghai

ENTRYPOINT ["java", "-jar", "-Xms256m", "-Xmx512m", "/app/app.jar"]
