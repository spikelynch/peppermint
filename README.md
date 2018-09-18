# Peppermint

JSON data transformer for [ReDBox](http://redboxresearchdata.com.au/), etc.

## Requirements

- JRE installation
- A Groovy installation

## Running

### Via Docker
- Install [docker-compose](https://docs.docker.com/compose/install/)
- Run: `mkdir /mnt/data/solr; chown 8983:8983 /mnt/data/solr`
- From the project directory, run: `docker-compose up`

### Via manual run

Build, the project (see below), then run command from project directory: `java -cp <path to groovy installation>/lib/*:./build/libs/peppermint-fat.jar io.vertx.core.Launcher`

## Building
Run command: ` ./gradlew shadowJar`
