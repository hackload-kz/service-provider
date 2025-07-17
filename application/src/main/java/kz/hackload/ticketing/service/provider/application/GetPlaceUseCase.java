package kz.hackload.ticketing.service.provider.application;

import java.util.List;
import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public interface GetPlaceUseCase
{
    Optional<GetPlaceQueryResult> getPlace(final PlaceId placeId);

    List<GetPlaceQueryResult> getPlaces(final int page, final int pageSize);
}
