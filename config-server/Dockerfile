FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

COPY target/config-server-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8888

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

