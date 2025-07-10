package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class ReleasePlaceApplicationService implements ReleasePlaceUseCase
{
    private final TransactionManager transactionManager;

    private final OrdersRepository ordersRepository;
    private final PlacesRepository placesRepository;

    private final ReleasePlaceService releasePlaceService;
    private final EventsDispatcher eventsDispatcher;

    public ReleasePlaceApplicationService(final TransactionManager transactionManager,
                                          final OrdersRepository ordersRepository,
                                          final PlacesRepository placesRepository,
                                          final ReleasePlaceService releasePlaceService,
                                          final EventsDispatcher eventsDispatcher)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
        this.placesRepository = placesRepository;
        this.releasePlaceService = releasePlaceService;
        this.eventsDispatcher = eventsDispatcher;
    }

    @Override
    public void releasePlace(final PlaceId placeId) throws PlaceAlreadyReleasedException, PlaceNotAddedException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final OrderId orderId = place.selectedFor().orElseThrow(() -> new PlaceNotAddedException(placeId));

        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        releasePlaceService.release(order, place);

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(placeId, place.uncommittedEvents());
            placesRepository.save(place);
        });
    }
}
