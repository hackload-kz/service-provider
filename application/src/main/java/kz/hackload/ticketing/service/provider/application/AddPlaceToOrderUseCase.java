package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface AddPlaceToOrderUseCase
{
    void addPlaceToOrder(PlaceId placeId, OrderId orderId) throws OrderNotStartedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, AggregateRestoreException;
}
