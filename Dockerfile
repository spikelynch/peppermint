FROM vertx/vertx3

ENV VERTICLE_NAME Main.groovy
ENV VERTICLE_HOME /opt/peppermint
EXPOSE 8080

COPY src/main/groovy/ $VERTICLE_HOME/
COPY scripts/ $VERTICLE_HOME/scripts/
COPY config.json $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]