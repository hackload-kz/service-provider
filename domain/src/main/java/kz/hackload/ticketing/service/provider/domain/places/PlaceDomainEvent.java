package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.DomainEvent;

public sealed interface PlaceDomainEvent extends DomainEvent permits PlaceCreatedEvent, PlaceReleasedEvent, PlaceSelectedEvent
{
}
