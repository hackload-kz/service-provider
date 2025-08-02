FROM eclipse-temurin:24.0.1_9-jre-ubi9-minimal

RUN mkdir /opt/app
RUN mkdir /opt/agents

COPY infrastructure/target/classes /opt/app/classes
COPY infrastructure/target/libs /opt/app/libs
COPY agents/opentelemetry-javaagent.jar /opt/agents/opentelemetry-javaagent.jar

ENV JAVA_OPTS=""
ENV JAVA_TOOL_OPTIONS="-javaagent:/opt/agents/opentelemetry-javaagent.jar "
ENV OTEL_SERVICE_NAME="service-provider"
ENV OTEL_EXPORTER_OTLP_PROTOCOL="grpc"
ENV OTEL_EXPORTER_OTLP_ENDPOINT="http://otel-collector:4317"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom --module-path /opt/app/classes:/opt/app/libs --module kz.hackload.ticketing.service.provider.infrastructure/kz.hackload.ticketing.service.provider.infrastructure.ApplicationRunner"]
