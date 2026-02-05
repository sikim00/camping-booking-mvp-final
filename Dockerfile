FROM gradle:8.9-jdk17 AS build
WORKDIR /app
ENV GRADLE_USER_HOME=/home/gradle/.gradle
COPY --chown=gradle:gradle . .
RUN gradle --no-daemon clean bootJar -x test

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
