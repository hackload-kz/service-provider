package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.places.*;

public final class CreatePlaceApplicationService implements CreatePlaceUseCase
{
    private final TransactionManager transactionManager;
    private final PlacesRepository repository;

    public CreatePlaceApplicationService(final TransactionManager transactionManager, final PlacesRepository repository)
    {
        this.transactionManager = transactionManager;
        this.repository = repository;
    }

    @Override
    public PlaceId create(final Row row, Seat seat)
    {
        final PlaceId placeId = repository.nextId();
        final Place place = Place.create(placeId, row, seat);

        transactionManager.executeInTransaction(() -> repository.save(place));

        return placeId;
    }
}
