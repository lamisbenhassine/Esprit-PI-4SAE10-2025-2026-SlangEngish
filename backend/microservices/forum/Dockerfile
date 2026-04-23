FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
COPY src src

RUN ./mvnw -DskipTests clean package

FROM eclipse-temurin:17-jre
WORKDIR /app

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8040

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
