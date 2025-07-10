package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class RemovePlaceFromOrderFromOrderApplicationService implements RemovePlaceFromOrderUseCase
{
    private final TransactionManager transactionManager;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;
    private final EventsDispatcher eventsDispatcher;

    private final RemovePlaceFromOrderService removePlaceFromOrderService;

    public RemovePlaceFromOrderFromOrderApplicationService(final TransactionManager transactionManager,
                                                           final PlacesRepository placesRepository,
                                                           final OrdersRepository ordersRepository,
                                                           final EventsDispatcher eventsDispatcher,
                                                           final RemovePlaceFromOrderService removePlaceFromOrderService)
    {
        this.transactionManager = transactionManager;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
        this.eventsDispatcher = eventsDispatcher;
        this.removePlaceFromOrderService = removePlaceFromOrderService;
    }

    @Override
    public void removePlaceFromOrder(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException, PlaceSelectedForAnotherOrderException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final OrderId orderId = place.selectedFor().orElseThrow();
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        removePlaceFromOrderService.removePlace(order, place);

        transactionManager.executeInTransaction(() ->
        {
            ordersRepository.save(order);
            eventsDispatcher.dispatch(orderId, order.uncommittedEvents());
        });
    }
}
