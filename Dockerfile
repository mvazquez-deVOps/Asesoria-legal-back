# Etapa 1: Compilación
FROM gradle:8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN chmod +x gradlew
RUN ./gradlew clean bootJar -x test


# Etapa 2: Ejecución
FROM eclipse-temurin:17-jdk-alpine
ENV PORT=8080
EXPOSE 8080
COPY  --from=build /home/gradle/src/build/libs/*.jar app.jar
ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT} -jar /app.jar"]
