# Recipe under test: production JVM options in the image, shared by the training run and the container.
FROM cdsdemo:base
ENV JAVA_TOOL_OPTIONS="-XX:+UseZGC"
RUN ["java", "-XX:ArchiveClassesAtExit=/app/app.jsa", "-Dspring.context.exit=onRefresh", "-jar", "/app/cdsdemo-0.0.1-SNAPSHOT.jar"]
ENTRYPOINT ["java", "-XX:SharedArchiveFile=/app/app.jsa", "-jar", "/app/cdsdemo-0.0.1-SNAPSHOT.jar"]
