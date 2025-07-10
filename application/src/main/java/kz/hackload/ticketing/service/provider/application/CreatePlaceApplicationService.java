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
    private final EventsDispatcher eventsDispatcher;

    public CreatePlaceApplicationService(final Clocks clocks,
                                         final TransactionManager transactionManager,
                                         final PlacesRepository repository,
                                         final EventsDispatcher eventsDispatcher)
    {
        this.clocks = clocks;
        this.transactionManager = transactionManager;
        this.repository = repository;
        this.eventsDispatcher = eventsDispatcher;
    }

    @Override
    public PlaceId create(final Row row, Seat seat)
    {
        final PlaceId placeId = repository.nextId();
        final Place place = Place.create(clocks.now(), placeId, row, seat);

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(placeId, place.uncommittedEvents());
            repository.save(place);
        });

        return placeId;
    }
}
