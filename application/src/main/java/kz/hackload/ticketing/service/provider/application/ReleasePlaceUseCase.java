package kz.hackload.ticketing.service.provider.application;

import java.util.Set;

import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface ReleasePlaceUseCase
{
    void releasePlace(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyReleasedException;

    void releasePlacesFromSingleOrder(final Set<PlaceId> placeIds) throws PlaceAlreadyReleasedException;
}
