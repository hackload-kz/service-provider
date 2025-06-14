package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class AddPlaceToOrderService
{
    public void addPlace(final Order order, final Place place) throws PlaceAlreadyAddedException, PlaceSelectedForAnotherOrderException, PlaceIsNotSelectedException, OrderNotStartedException
    {
        final OrderId orderId = order.id();
        final PlaceId placeId = place.id();

        final Optional<OrderId> selectedFor = place.selectedFor();
        if (selectedFor.isEmpty())
        {
            throw new PlaceIsNotSelectedException(placeId, orderId);
        }

        if (!place.isSelectedFor(orderId))
        {
            throw new PlaceSelectedForAnotherOrderException(placeId, selectedFor.get(), orderId);
        }

        order.addPlace(placeId);
    }
}
