package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;

public final class SelectPlaceApplicationService implements SelectPlaceUseCase
{
    private final SelectPlaceService selectPlaceService;

    private final TransactionManager transactionManager;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    private final EventsDispatcher eventsDispatcher;

    public SelectPlaceApplicationService(final SelectPlaceService selectPlaceService,
                                         final TransactionManager transactionManager,
                                         final PlacesRepository placesRepository,
                                         final OrdersRepository ordersRepository,
                                         final EventsDispatcher eventsDispatcher)
    {
        this.selectPlaceService = selectPlaceService;
        this.transactionManager = transactionManager;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
        this.eventsDispatcher = eventsDispatcher;
    }

    @Override
    public void selectPlaceFor(final PlaceId placeId, final OrderId orderId) throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        selectPlaceService.selectPlaceForOrder(place, order);

        transactionManager.executeInTransaction(() ->
        {
            placesRepository.save(place);
            eventsDispatcher.dispatch(placeId, place.uncommittedEvents());
        });
    }
}
