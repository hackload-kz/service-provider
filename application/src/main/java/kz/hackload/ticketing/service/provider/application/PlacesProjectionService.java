package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.places.PlaceCreatedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceReleasedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceSelectedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlacesProjectionsRepository;

public final class PlacesProjectionService
{
    private final PlacesProjectionsRepository placesProjectionsRepository;

    public PlacesProjectionService(final PlacesProjectionsRepository placesProjectionsRepository)
    {
        this.placesProjectionsRepository = placesProjectionsRepository;
    }

    public void placeCreated(final PlaceId placeId, final PlaceCreatedEvent placeCreatedEvent)
    {
        placesProjectionsRepository.placeCreated(placeId, placeCreatedEvent.row(), placeCreatedEvent.seat());
    }

    public void placeSelected(final PlaceId placeId, final PlaceSelectedEvent e)
    {
        placesProjectionsRepository.placeSelected(placeId);
    }

    public void placeReleased(final PlaceId placeId, final PlaceReleasedEvent e)
    {
        placesProjectionsRepository.placeReleased(placeId);
    }
}
