FROM eclipse-temurin:21.0.4_7-jre-jammy

LABEL org.opencontainers.image.authors=nantaaditya
LABEL org.opencontainers.image.url=https://hub.docker.com/repository/docker/nantaaditya/cron-scheduler/
LABEL org.opencontainers.image.source=https://github.com/nantaaditya/cron-scheduler
LABEL org.opencontainers.image.title=cron-scheduler
LABEL org.opencontainers.image.version=v.0.1.3
LABEL JDK_VERSION=17
LABEL JDK_DISTRIBUTION=eclipse-temurin
LABEL JDK_BUILD_VERSION=17.0.11_9-jre-alpine
LABEL SPRING_VERSION=3.1.5

ENV TZ=Asia/Jakarta
ENV JAVA_OPTS='-Xms256m -Xmx512m'

RUN mkdir app
RUN mkdir app/logs

ADD target/*.jar app/app.jar

WORKDIR /app

ENTRYPOINT exec java -server -XX:+UnlockExperimentalVMOptions $JAVA_OPTS -jar app.jar
