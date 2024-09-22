FROM openjdk:17-alpine
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} order-service.jar
ENTRYPOINT ["java","-jar","order-service.jar"]