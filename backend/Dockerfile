# Build stage
FROM eclipse-temurin:26-jdk AS build
WORKDIR /app

# Grant execute permission to the gradlew script
COPY gradlew /app/
RUN chmod +x ./gradlew

# Copy the rest of the gradle wrapper
COPY gradle /app/gradle

# Copy the build configuration files
COPY build.gradle settings.gradle /app/

# Download dependencies to leverage Docker cache
RUN ./gradlew dependencies --no-daemon

# Copy the source code
COPY src /app/src

# Build the application, creating the bootable JAR
RUN ./gradlew bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:26-jre
WORKDIR /app

# Copy the executable JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Expose the standard application port
EXPOSE 8080

# Add a health check to verify the application is running
HEALTHCHECK --interval=30s --timeout=3s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application with the production profile active
ENV SPRING_PROFILES_ACTIVE=production
ENTRYPOINT ["java", "-jar", "app.jar"]

