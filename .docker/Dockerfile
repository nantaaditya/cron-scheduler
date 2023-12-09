FROM eclipse-temurin:17-jre-alpine

MAINTAINER nantaaditya

LABEL CREATOR=nantaaditya
LABEL PROJECT_NAME=cron-scheduler
LABEL JDK_VERSION=17
LABEL JDK_DISTRIBUTION=eclipse-temurin
LABEL JDK_BUILD_VERSION=17-jre-alpine
LABEL SPRING_VERSION=3.1.5

ENV TZ=Asia/Jakarta
ENV JAVA_OPTS='-Xms256m -Xmx512m'

RUN mkdir app
RUN mkdir app/logs

ADD target/*.jar app/app.jar

WORKDIR /app

ENTRYPOINT exec java -server -XX:+UnlockExperimentalVMOptions $JAVA_OPTS -jar app.jar