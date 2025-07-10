package kz.hackload.ticketing.service.provider.domain.places;

public interface PlacesProjectionsRepository
{
    void placeCreated(final PlaceId placeId, final Row row, final Seat seat);

    void placeSelected(final PlaceId placeId);

    void placeReleased(final PlaceId placeId);
}
