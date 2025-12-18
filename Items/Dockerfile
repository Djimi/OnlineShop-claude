FROM eclipse-temurin:25-jre-alpine

RUN apk --no-cache add curl

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]
