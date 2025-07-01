package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface SelectPlaceUseCase
{
    void selectPlaceFor(final PlaceId placeId, final OrderId orderId) throws PlaceAlreadySelectedException,
            PlaceCanNotBeAddedToOrderException;
}
