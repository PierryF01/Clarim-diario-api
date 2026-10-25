FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /build

COPY api/pom.xml api/mvnw ./
COPY api/.mvn .mvn

RUN chmod +x mvnw && ./mvnw -B --no-transfer-progress dependency:go-offline

COPY api/src ./src

RUN ./mvnw -B --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring

WORKDIR /app

COPY --from=builder --chown=spring:spring /build/target/*.jar app.jar

USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]