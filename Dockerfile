# Use OpenJDK base image
FROM openjdk:17-jdk-slim

# Set working directory inside container
WORKDIR /app

# Copy Maven build output (your jar)
COPY target/*.jar app.jar

# Expose app port
EXPOSE 3000

# Command to run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
