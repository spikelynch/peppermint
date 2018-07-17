FROM openjdk:8-jdk-slim

ENV APP_HOME /opt/peppermint
EXPOSE 8080

COPY gradlew $APP_HOME/
COPY gradle $APP_HOME/gradle
COPY build.gradle $APP_HOME/
COPY settings.gradle $APP_HOME/
COPY src $APP_HOME/src

RUN cd $APP_HOME && ./gradlew shadowJar

COPY scripts/ $APP_HOME/scripts/
COPY config.json $APP_HOME/

WORKDIR $APP_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec java -jar build/libs/peppermint-fat.jar"]