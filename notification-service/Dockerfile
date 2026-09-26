# Build context: repo kökü (common-dto'ya erişim için).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY common-dto/pom.xml common-dto/pom.xml
COPY common-dto/src common-dto/src
RUN mvn -q -f common-dto/pom.xml install
COPY notification-service/pom.xml notification-service/pom.xml
RUN mvn -q -f notification-service/pom.xml dependency:go-offline
COPY notification-service/src notification-service/src
RUN mvn -q -f notification-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/notification-service/target/notification-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
