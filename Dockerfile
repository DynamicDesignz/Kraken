FROM openjdk:8-jre

# Add Aircrack
RUN apt-get update && apt-get install --yes aircrack-ng && apt-get --yes install crunch && rm -rf /var/lib/apt/lists/*

# Add a kraken user to run our application so that it doesn't need to run as root
RUN useradd -ms /bin/bash kraken
WORKDIR /home/kraken

# Copy the jar file from build into the container
COPY ./build/libs/Kraken-0.0.1.jar Kraken.jar

CMD ["java", "-jar", "-Dspring.profiles.active=production", "Kraken.jar"]

EXPOSE 5000