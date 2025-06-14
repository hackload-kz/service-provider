package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record PlaceRemovedFromOrderEvent(PlaceId placeId) implements OrderDomainEvent
{
}
