package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface CreatePlaceUseCase
{
    void create(final PlaceId placeId);
}
