package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.AddPlaceToOrderService;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class AddPlaceToOrderApplicationService implements AddPlaceToOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;
    private final PlacesRepository placesRepository;
    private final AddPlaceToOrderService addPlaceToOrderService;
    private final EventsDispatcher eventsDispatcher;

    public AddPlaceToOrderApplicationService(final TransactionManager transactionManager,
                                             final OrdersRepository ordersRepository,
                                             final PlacesRepository placesRepository,
                                             final AddPlaceToOrderService addPlaceToOrderService,
                                             final EventsDispatcher eventsDispatcher)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
        this.placesRepository = placesRepository;
        this.addPlaceToOrderService = addPlaceToOrderService;
        this.eventsDispatcher = eventsDispatcher;
    }

    @Override
    public void addPlaceToOrder(final PlaceId placeId, final OrderId orderId) throws OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());

        addPlaceToOrderService.addPlace(order, place);

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(orderId, order.uncommittedEvents());
            ordersRepository.save(order);
        });
    }
}
