FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn -DskipTests package

FROM eclipse-temurin:17-jre

RUN apt-get update \
  && apt-get install -y --no-install-recommends default-mysql-client \
  && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8011

CMD ["sh","-c","until mysqladmin ping -h \"$DB_HOST\" -P \"$DB_PORT\" -u \"$DB_USER\" -p\"$DB_PASSWORD\" --silent; do echo \"Waiting for MySQL...\"; sleep 2; done; java -jar /app/app.jar"]

