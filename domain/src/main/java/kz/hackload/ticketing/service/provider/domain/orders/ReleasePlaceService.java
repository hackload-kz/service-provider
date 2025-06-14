package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public class ReleasePlaceService
{
    public void release(final Order order, final Place place) throws
            OrderNotStartedException,
            PlaceNotAddedException
    {
        final PlaceId placeId = place.id();

        order.removePlace(placeId);
    }
}
