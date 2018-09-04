# Peppermint

JSON data transformer for [ReDBox](http://redboxresearchdata.com.au/), etc.

## Requirements

- JRE installation
- A Groovy installation

## Running

### Via Docker
- Install [docker-compose](https://docs.docker.com/compose/install/)
- From the project directory, run: `docker-compose up`

### Via manual run (from the dev environment, etc.)

Build, the project (see below), then run command from project directory: `java -cp <path to groovy installation>/lib/*:./build/libs/peppermint-fat.jar io.vertx.core.Launcher`

## Building
Run command: ` ./gradlew shadowJar`
