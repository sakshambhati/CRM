FROM docker.io/library/openjdk:17-jdk-alpine
COPY ./target/CRM-0.0.1-SNAPSHOT.jar CRM-0.0.1-SNAPSHOT.jar
COPY ./Tess4J /Tess4J
CMD ["java","-jar","CRM-0.0.1-SNAPSHOT.jar"]