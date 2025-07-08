package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record PlaceRemovedFromOrderEvent(Instant occurredOn, long revision, PlaceId placeId) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "place_removed_from_order_event";
    }
}
