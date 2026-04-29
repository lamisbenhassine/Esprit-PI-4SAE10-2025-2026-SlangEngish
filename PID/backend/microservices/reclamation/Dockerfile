# Image runtime uniquement : le JAR est déjà construit par Jenkins (étape Package) avec mvnw package.
# Évite de relancer Maven dans Docker (plus de longs téléchargements "Downloading from central" à chaque build).
FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

EXPOSE 8012

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
