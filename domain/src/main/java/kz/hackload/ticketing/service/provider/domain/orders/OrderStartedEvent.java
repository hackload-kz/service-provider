package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

public record OrderStartedEvent(Instant occurredOn, long revision) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_started_event";
    }
}
