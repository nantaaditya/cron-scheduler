FROM eclipse-temurin:17-jre-alpine

ENV TZ=Asia/Jakarta

RUN mkdir app
RUN mkdir app/logs

ADD target/*.jar app/app.jar

WORKDIR /app

ENTRYPOINT [ "java","-jar","app.jar" ]