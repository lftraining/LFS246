FROM openjdk:17-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} customer-service.jar
ENTRYPOINT ["java","-jar","customer-service.jar"]