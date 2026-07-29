# Step 1: Build the Java Spring Boot application using Maven
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Run the application using OpenJDK 17
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/cardiopulse-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
