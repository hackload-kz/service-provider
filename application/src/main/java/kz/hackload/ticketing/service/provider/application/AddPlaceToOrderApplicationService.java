package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class AddPlaceToOrderApplicationService implements AddPlaceToOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;
    private final PlacesRepository placesRepository;
    private final AddPlaceToOrderService addPlaceToOrderService;

    public AddPlaceToOrderApplicationService(final TransactionManager transactionManager,
                                             final OrdersRepository ordersRepository,
                                             final PlacesRepository placesRepository,
                                             final AddPlaceToOrderService addPlaceToOrderService)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
        this.placesRepository = placesRepository;
        this.addPlaceToOrderService = addPlaceToOrderService;
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

        transactionManager.executeInTransaction(() -> ordersRepository.save(order));
    }
}
