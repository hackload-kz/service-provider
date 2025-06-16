package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.places.*;

public final class CreatePlaceApplicationService implements CreatePlaceUseCase
{
    private final PlacesRepository repository;

    public CreatePlaceApplicationService(final PlacesRepository repository)
    {
        this.repository = repository;
    }

    @Override
    public void create(final PlaceId placeId, final Row row, Seat seat)
    {
        final Place place = Place.create(placeId, row, seat);
        repository.save(place);
    }
}
