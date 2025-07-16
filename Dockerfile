FROM eclipse-temurin:24.0.1_9-jre-ubi9-minimal
RUN mkdir /opt/app

COPY infrastructure/target/classes /opt/app/classes
COPY infrastructure/target/libs /opt/app/libs

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS  -Djava.security.egd=file:/dev/./urandom --module-path /opt/app/classes:/opt/app/libs --module kz.hackload.ticketing.service.provider.infrastructure/kz.hackload.ticketing.service.provider.infrastructure.ApplicationRunner"]
