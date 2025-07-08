package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class ReleasePlaceService
{
    private final Clocks clocks;

    public ReleasePlaceService(final Clocks clocks)
    {
        this.clocks = clocks;
    }

    public void release(final Order order, final Place place) throws PlaceAlreadyReleasedException
    {
        final PlaceId placeId = place.id();

        if (order.contains(placeId))
        {
            // replace with domain exception
            throw new RuntimeException();
        }

        place.release(clocks.now());
    }
}
