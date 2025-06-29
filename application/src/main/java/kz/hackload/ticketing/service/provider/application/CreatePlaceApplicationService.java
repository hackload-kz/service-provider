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
    public PlaceId create(final Row row, Seat seat)
    {
        final PlaceId placeId = repository.nextId();
        final Place place = Place.create(placeId, row, seat);
        repository.save(place);
        return placeId;
    }
}
