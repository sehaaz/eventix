# Build context: repo kökü (common-dto'ya erişim için).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY common-dto/pom.xml common-dto/pom.xml
COPY common-dto/src common-dto/src
RUN mvn -q -f common-dto/pom.xml install
COPY event-service/pom.xml event-service/pom.xml
RUN mvn -q -f event-service/pom.xml dependency:go-offline
COPY event-service/src event-service/src
RUN mvn -q -f event-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/event-service/target/event-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
