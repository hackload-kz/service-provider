package kz.hackload.ticketing.service.provider.domain.places;

import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public record PlaceSelectedEvent(Instant occurredOn, long revision, OrderId orderId) implements PlaceDomainEvent
{
    @Override
    public String type()
    {
        return "place_selected_event";
    }
}
