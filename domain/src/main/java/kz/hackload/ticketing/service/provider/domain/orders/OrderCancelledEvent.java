package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;
import java.util.Set;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record OrderCancelledEvent(Instant occurredOn, long revision, Set<PlaceId> placeId) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_cancelled_event";
    }
}
