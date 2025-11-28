# Use OpenJDK 17 slim base
FROM openjdk:17

# Set working directory
WORKDIR /app

# Copy Maven build output (jar)
COPY target/*.jar app.jar

# Expose app port
EXPOSE 3000

# Run the app
ENTRYPOINT ["java", "-jar", "app.jar"]
