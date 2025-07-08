package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

public record OrderConfirmedEvent(Instant occurredOn, long revision) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_confirmed_event";
    }
}
