module kz.hackload.ticketing.service.provider.domain
{
    requires org.jspecify;

    exports kz.hackload.ticketing.service.provider.domain.orders to kz.hackload.ticketing.service.provider.application, kz.hackload.ticketing.service.provider.infrastructure, com.fasterxml.jackson.databind;
    exports kz.hackload.ticketing.service.provider.domain.places to kz.hackload.ticketing.service.provider.application, kz.hackload.ticketing.service.provider.infrastructure, com.fasterxml.jackson.databind;
    exports kz.hackload.ticketing.service.provider.domain.outbox to kz.hackload.ticketing.service.provider.application, kz.hackload.ticketing.service.provider.infrastructure, com.fasterxml.jackson.databind;
    exports kz.hackload.ticketing.service.provider.domain to kz.hackload.ticketing.service.provider.application, kz.hackload.ticketing.service.provider.infrastructure, com.fasterxml.jackson.databind;
}
