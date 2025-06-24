package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.Set;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record OrderCancelledEvent(Set<PlaceId> placeId) implements OrderDomainEvent
{
    @Override
    public String type()
    {
        return "order_cancelled_event";
    }
}
