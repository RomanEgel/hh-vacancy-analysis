FROM openjdk:15
ARG LIB_FILE=stat-calculator/build/libs/*.jar
COPY ${LIB_FILE} statCount.jar
ARG JAR_FILE=api-service/build/libs/*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java","-jar","/app.jar"]