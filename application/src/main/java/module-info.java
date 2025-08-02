module kz.hackload.ticketing.service.provider.application
{
    requires org.jspecify;
    requires org.slf4j;
    requires io.opentelemetry.instrumentation_annotations;

    requires kz.hackload.ticketing.service.provider.domain;
    requires io.opentelemetry.api;

    exports kz.hackload.ticketing.service.provider.application to kz.hackload.ticketing.service.provider.infrastructure;
}
