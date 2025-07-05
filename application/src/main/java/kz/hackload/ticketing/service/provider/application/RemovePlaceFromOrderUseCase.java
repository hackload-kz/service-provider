package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface RemovePlaceFromOrderUseCase
{
    void removePlaceFromOrder(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException, PlaceSelectedForAnotherOrderException;
}
