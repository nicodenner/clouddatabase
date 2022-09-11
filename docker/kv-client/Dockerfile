# select parent image
FROM maven:3.6.3-jdk-8

# copy the source tree and the pom.xml to our new container
COPY ./ ./

# package our application code
RUN mvn clean package

# set the startup command to execute the jar
ENTRYPOINT ["java", "-jar", "target/kv-client.jar"]