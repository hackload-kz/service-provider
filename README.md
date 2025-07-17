# RTFM

## Local run

### Linux

#### Prerequisites:
1. Java 24. You can use https://sdkman.io/
2. Docker and docker-compose https://docs.docker.com/engine/install/ https://docs.docker.com/compose/install/

#### Steps:
1. Build maven modules
    ```shell
    sh build.sh
    ```
2. Run docker-compose
    ```shell
    docker-compose --file docker-compose/docker-compose.yaml up --detach
    ```

TO BE DONE...