FROM eclipse-temurin:25.0.1_8-jre-alpine

RUN apk --no-cache add curl

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 10000

ENTRYPOINT ["java", "-jar", "app.jar"]
