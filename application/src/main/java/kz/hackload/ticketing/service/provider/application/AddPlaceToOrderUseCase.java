package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface AddPlaceToOrderUseCase
{
    void addPlaceToOrder(PlaceId placeId, OrderId orderId) throws OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException;
}
