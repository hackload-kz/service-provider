package kz.hackload.ticketing.service.provider.application;

import java.util.List;
import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesQueryRepository;

public final class PlacesQueryService implements GetPlaceUseCase
{
    private final PlacesQueryRepository placesQueryRepository;

    public PlacesQueryService(final PlacesQueryRepository placesQueryRepository)
    {
        this.placesQueryRepository = placesQueryRepository;
    }

    @Override
    public Optional<GetPlaceQueryResult> getPlace(final PlaceId placeId)
    {
        return placesQueryRepository.getPlace(placeId);
    }

    @Override
    public List<GetPlaceQueryResult> getPlaces(final int page, final int pageSize)
    {
        return placesQueryRepository.getPlaces(page, pageSize);
    }
}
