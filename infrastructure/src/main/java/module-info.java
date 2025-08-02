module kz.hackload.ticketing.service.provider.infrastructure
{
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires com.zaxxer.hikari;
    requires java.sql;
    requires io.javalin;
    requires kafka.clients;
    requires kz.hackload.ticketing.service.provider.application;
    requires kz.hackload.ticketing.service.provider.domain;
    requires org.jspecify;
    requires org.postgresql.jdbc;
    requires org.slf4j;
    requires io.opentelemetry.instrumentation_annotations;
    requires io.opentelemetry.api;

    exports kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http to com.fasterxml.jackson.databind;
    exports kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka to com.fasterxml.jackson.databind;
    exports kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto to com.fasterxml.jackson.databind;
}
