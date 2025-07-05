module kz.hackload.ticketing.service.provider.infrastructure {
    requires org.jspecify;
    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires io.javalin;
    requires java.sql;
    requires kafka.clients;
    requires kz.hackload.ticketing.service.provider.application;
    requires kz.hackload.ticketing.service.provider.domain;
    requires org.postgresql.jdbc;
    requires org.slf4j;
}