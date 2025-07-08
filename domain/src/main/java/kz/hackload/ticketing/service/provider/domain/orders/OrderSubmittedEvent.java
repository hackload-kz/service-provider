package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

public record OrderSubmittedEvent(Instant occurredOn, long revision) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_submitted_event";
    }
}
