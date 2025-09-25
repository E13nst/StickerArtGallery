# Use an official OpenJDK runtime as a parent image
FROM openjdk:17-jdk

# Set the working directory in the container
WORKDIR /app

# Copy the application's jar file to the container
COPY build/libs/telegram-bot-dream-stream-0.0.1-SNAPSHOT.jar /app/telegram-bot-dream-stream.jar

# Copy the external application.yaml file into the Docker container
COPY src/main/resources/application.yaml /app/config/application.yaml

# Copy the additional file to the container
# COPY prompt.txt /app/prompt.txt

# Command to run the jar file
CMD ["java", "-jar", "/app/telegram-bot-dream-stream.jar", "--spring.config.location=file:/app/config/application.yaml"]