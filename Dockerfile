FROM openjdk:8-jre

# Add a temporary volume
VOLUME /tmp

# Add Aircrack
RUN apt-get update && apt-get install aircrack-ng && apt-get install crunch && rm -rf /var/lib/apt/lists/*

# Add a kraken user to run our application so that it doesn't need to run as root
RUN adduser -D -s /bin/sh kraken
WORKDIR /home/kraken

# Copy the jar file from build into the container
COPY ./build/libs/Kraken-0.0.1.jar Kraken.jar

# Copy the run script
COPY ./run.sh run.sh

ENTRYPOINT ["./run.sh"]

EXPOSE 5000