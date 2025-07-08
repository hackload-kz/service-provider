package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record PlaceAddedToOrderEvent(Instant occurredOn, long revision, PlaceId placeId) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "place_added_to_order_event";
    }
}
