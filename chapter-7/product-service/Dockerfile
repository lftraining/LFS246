FROM openjdk:11-jre-slim
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} product-service.jar
ENTRYPOINT ["java","-jar","product-service.jar"]