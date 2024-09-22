FROM openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} customer-service.jar
ENTRYPOINT ["java","-jar","customer-service.jar"]