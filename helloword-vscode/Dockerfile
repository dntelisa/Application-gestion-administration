# app build
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# runnable image
FROM eclipse-temurin:17-jdk-alpine
VOLUME /tmp
WORKDIR /app

# generated jar copy
COPY --from=build /app/target/*.jar app.jar

# keystore SSL copy
COPY src/main/resources/keystore.jks keystore.jks

EXPOSE 8443
ENTRYPOINT ["java", "-jar", "app.jar"]
