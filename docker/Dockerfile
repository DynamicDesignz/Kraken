FROM openjdk:8-jre-alpine

# Add a temporary volume
VOLUME /tmp

# Add a kraken user to run our application so that it doesn't need to run as root
RUN adduser -D -s /bin/sh kraken
WORKDIR /home/kraken

# Defining the jar that will come to this file as an argument
ARG JAR_FILE

# Copy the jar file into the container
COPY ${JAR_FILE} kraken.jar

ENTRYPOINT ["java", "-jar", "kraken.jar"]

RUN ls -l

EXPOSE 5000