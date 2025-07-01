package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface ReleasePlaceUseCase
{
    void releasePlace(final PlaceId placeId) throws PlaceAlreadyReleasedException, OrderNotStartedException, PlaceNotAddedException;
}
