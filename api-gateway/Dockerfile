FROM eclipse-temurin:21-jre-alpine

RUN apk add --no-cache wget

WORKDIR /app

COPY target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8765

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
