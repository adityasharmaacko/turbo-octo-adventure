# Step 1: Build Maven dependencies
FROM maven:3.6.3-jdk-11-slim AS dependency
WORKDIR /usr/app
COPY pom.xml .
RUN mvn dependency:go-offline -B --fail-never

# Step 2: Build the application
FROM dependency as build
WORKDIR /usr/app
COPY src src
COPY src/main/resources resources
RUN --mount=type=cache,target=/root/.m2 mvn clean install -U -DskipTests=true

# Step 3: Prepare the final runtime image
FROM openjdk:11-jre-slim
WORKDIR /usr/app

# Copy the application JAR
COPY --from=build /usr/app/target/*.jar app.jar

# Configure environment variables for OR-Tools
ENV LD_LIBRARY_PATH=/opt/or-tools/lib:$LD_LIBRARY_PATH
ENV ORTOOLS_JAR_PATH=/usr/app/app.jar

# Expose the application port
EXPOSE 8003

# Run the application with OR-Tools native libraries
CMD ["java", "-jar", "-Dspring.profiles.active=local", "app.jar"]
