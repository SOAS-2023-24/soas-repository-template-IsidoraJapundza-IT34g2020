FROM eclipse-temurin:17-jre
WORKDIR /app
EXPOSE 8000
COPY target/*.jar /app/app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
