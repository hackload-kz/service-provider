package kz.hackload.ticketing.service.provider.domain.places;

import java.time.Instant;

public record PlaceCreatedEvent(Instant occurredOn, long revision, Row row, Seat seat) implements PlaceDomainEvent
{
    @Override
    public String type()
    {
        return "place_created_event";
    }
}
