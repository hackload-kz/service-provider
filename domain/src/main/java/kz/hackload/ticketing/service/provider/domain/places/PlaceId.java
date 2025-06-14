package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.DomainEntityId;

public record PlaceId(Row row, Seat seat) implements DomainEntityId
{
}
