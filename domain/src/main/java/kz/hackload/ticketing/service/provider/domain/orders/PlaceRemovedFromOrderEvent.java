package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record PlaceRemovedFromOrderEvent(PlaceId placeId) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "place_removed_from_order_event";
    }
}
