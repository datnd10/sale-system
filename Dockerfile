# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Copy source code
COPY . .

# Build project (skip test cho nhanh)
RUN mvn clean package -DskipTests

# Stage 2: Run
FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# Copy file jar từ stage build
COPY --from=build /app/target/*.jar app.jar

# Expose port
EXPOSE 8080

# Run app
ENTRYPOINT ["java","-Xmx256m","-jar","app.jar"]