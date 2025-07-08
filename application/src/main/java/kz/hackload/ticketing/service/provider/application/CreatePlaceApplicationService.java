package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public final class CreatePlaceApplicationService implements CreatePlaceUseCase
{
    private final Clocks clocks;
    private final TransactionManager transactionManager;
    private final PlacesRepository repository;

    public CreatePlaceApplicationService(final Clocks clocks,
                                         final TransactionManager transactionManager,
                                         final PlacesRepository repository)
    {
        this.clocks = clocks;
        this.transactionManager = transactionManager;
        this.repository = repository;
    }

    @Override
    public PlaceId create(final Row row, Seat seat)
    {
        final PlaceId placeId = repository.nextId();
        final Place place = Place.create(clocks.now(), placeId, row, seat);

        transactionManager.executeInTransaction(() -> repository.save(place));

        return placeId;
    }
}
