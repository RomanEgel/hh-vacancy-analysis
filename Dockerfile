FROM openjdk:15
ARG LIB_FILE=StatCalculator/build/libs/*.jar
COPY ${LIB_FILE} statCount.jar
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]