package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface GetPlaceUseCase
{
    Optional<GetPlaceQueryResult> getPlace(final PlaceId placeId);
}
