FROM eclipse-temurin:21-jre
ARG MODULE
ARG VERSION
LABEL org.opencontainers.image.version=$VERSION
WORKDIR /app
COPY ${MODULE}/target/${MODULE}-*.jar app.jar
ENTRYPOINT ["java", "-Xmx256m", "-jar", "/app/app.jar"]
CMD ["--server.port=8080"]
