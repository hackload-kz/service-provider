package kz.hackload.ticketing.service.provider.domain.places;

import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public record PlaceReleasedEvent(Instant occurredOn, long revision, OrderId orderId) implements PlaceDomainEvent
{
    @Override
    public String type()
    {
        return "place_released_event";
    }
}
