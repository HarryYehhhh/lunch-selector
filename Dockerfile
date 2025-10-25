# 第一階段：建構
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# 第二階段：執行（使用 Debian-based Temurin）
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/lunch-selector-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
