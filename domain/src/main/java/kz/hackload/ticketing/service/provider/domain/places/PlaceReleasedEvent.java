package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public record PlaceReleasedEvent(OrderId orderId) implements PlaceDomainEvent
{
}
