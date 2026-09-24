# Build context: repo kökü (common-dto'ya erişim için).
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY common-dto/pom.xml common-dto/pom.xml
COPY common-dto/src common-dto/src
RUN mvn -q -f common-dto/pom.xml install
COPY ticket-service/pom.xml ticket-service/pom.xml
RUN mvn -q -f ticket-service/pom.xml dependency:go-offline
COPY ticket-service/src ticket-service/src
RUN mvn -q -f ticket-service/pom.xml package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /build/ticket-service/target/ticket-service-0.0.1-SNAPSHOT.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
