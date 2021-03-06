FROM openjdk:8-jdk-alpine
MAINTAINER Alok Singh (alok.ku.singh@gmail.com)
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} /app.jar
EXPOSE 8080
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/urandom","-jar","/app.jar"]