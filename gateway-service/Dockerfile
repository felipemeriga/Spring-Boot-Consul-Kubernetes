FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/gateway-service-1.1.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 8080