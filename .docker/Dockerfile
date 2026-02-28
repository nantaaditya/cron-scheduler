FROM eclipse-temurin:21-jre-alpine-3.23

LABEL org.opencontainers.image.authors=nantaaditya
LABEL org.opencontainers.image.url=https://hub.docker.com/repository/docker/nantaaditya/cron-scheduler/
LABEL org.opencontainers.image.source=https://github.com/nantaaditya/cron-scheduler
LABEL org.opencontainers.image.title=cron-scheduler
LABEL org.opencontainers.image.version=v.0.1.4
LABEL JDK_VERSION=21
LABEL JDK_DISTRIBUTION=eclipse-temurin
LABEL JDK_BUILD_VERSION=21-jre-alpine-3.23
LABEL SPRING_VERSION=3.5.10

ENV TZ=Asia/Jakarta
ENV JAVA_OPTS='-Xms256m -Xmx512m'

RUN mkdir app
RUN mkdir app/logs

ADD target/*.jar app/app.jar

WORKDIR /app

ENTRYPOINT exec java -server -XX:+UnlockExperimentalVMOptions $JAVA_OPTS -jar app.jar
