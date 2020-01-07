FROM openjdk:8-jdk-alpine
VOLUME /tmp
ADD target/product-service-1.1.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
EXPOSE 9001