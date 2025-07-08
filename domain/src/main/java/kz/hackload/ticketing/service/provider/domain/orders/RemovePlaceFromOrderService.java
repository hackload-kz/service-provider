package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class RemovePlaceFromOrderService
{
    private final Clocks clocks;

    public RemovePlaceFromOrderService(final Clocks clocks)
    {
        this.clocks = clocks;
    }

    public void removePlace(final Order order, final Place place) throws PlaceNotAddedException, PlaceSelectedForAnotherOrderException, OrderNotStartedException
    {
        final OrderId orderId = order.id();
        final PlaceId placeId = place.id();

        if (!place.isSelectedFor(orderId))
        {
            throw new PlaceSelectedForAnotherOrderException(placeId, place.selectedFor().orElseThrow(), orderId);
        }

        order.removePlace(clocks.now(), placeId);
    }
}
