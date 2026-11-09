FROM gcr.io/distroless/java25-debian13:latest
WORKDIR /app
COPY build/libs/txdemo-0.0.1-SNAPSHOT.jar /app/txdemo.jar
ENTRYPOINT ["java", "-jar", "/app/txdemo.jar"]
