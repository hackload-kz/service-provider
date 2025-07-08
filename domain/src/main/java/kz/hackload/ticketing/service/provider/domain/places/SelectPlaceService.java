package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.orders.Order;

public final class SelectPlaceService
{
    private final Clocks clocks;

    public SelectPlaceService(final Clocks clocks)
    {
        this.clocks = clocks;
    }

    public void selectPlaceForOrder(final Place place, final Order order) throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException
    {
        if (!order.canAddPlace())
        {
            throw new PlaceCanNotBeAddedToOrderException(place.id(), order.id());
        }

        place.selectFor(clocks.now(), order.id());
    }
}
