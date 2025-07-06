module kz.hackload.ticketing.service.provider.application
{
    requires org.jspecify;
    requires org.slf4j;

    requires kz.hackload.ticketing.service.provider.domain;

    exports kz.hackload.ticketing.service.provider.application to kz.hackload.ticketing.service.provider.infrastructure;
}
