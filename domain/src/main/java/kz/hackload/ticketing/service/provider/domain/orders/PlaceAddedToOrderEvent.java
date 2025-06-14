package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public record PlaceAddedToOrderEvent(PlaceId placeId) implements OrderDomainEvent
{
}
