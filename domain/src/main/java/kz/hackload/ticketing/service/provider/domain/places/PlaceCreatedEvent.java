package kz.hackload.ticketing.service.provider.domain.places;

public record PlaceCreatedEvent(Row row, Seat seat) implements PlaceDomainEvent
{
    @Override
    public String type()
    {
        return "place_created_event";
    }
}
