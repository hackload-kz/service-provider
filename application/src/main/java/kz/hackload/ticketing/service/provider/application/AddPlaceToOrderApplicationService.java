package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class AddPlaceToOrderApplicationService implements AddPlaceToOrderUseCase
{
    private final OrdersRepository ordersRepository;
    private final PlacesRepository placesRepository;
    private final AddPlaceToOrderService addPlaceToOrderService;

    public AddPlaceToOrderApplicationService(final OrdersRepository ordersRepository,
                                             final PlacesRepository placesRepository,
                                             final AddPlaceToOrderService addPlaceToOrderService)
    {
        this.ordersRepository = ordersRepository;
        this.placesRepository = placesRepository;
        this.addPlaceToOrderService = addPlaceToOrderService;
    }

    @Override
    public void addPlaceToOrder(final PlaceId placeId, final OrderId orderId) throws OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            AggregateRestoreException
    {
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        final Place place = placesRepository.findById(placeId).orElseThrow();

        addPlaceToOrderService.addPlace(order, place);

        ordersRepository.save(order);
    }
}
