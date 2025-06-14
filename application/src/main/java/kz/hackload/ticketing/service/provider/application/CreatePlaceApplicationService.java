package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class CreatePlaceApplicationService implements CreatePlaceUseCase
{
    private final PlacesRepository repository;

    public CreatePlaceApplicationService(final PlacesRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public void create(final PlaceId placeId)
    {
        final Place place = Place.create(placeId);
        repository.save(place);
    }
}
