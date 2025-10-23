FROM eclipse-temurin:17-jre
WORKDIR /app
COPY target/*.jar /app/app.jar
EXPOSE 8765
ENTRYPOINT ["java","-jar","/app/app.jar"]
