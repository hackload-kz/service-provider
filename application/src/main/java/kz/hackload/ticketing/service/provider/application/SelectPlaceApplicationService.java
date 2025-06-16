package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public final class SelectPlaceApplicationService implements SelectPlaceUseCase
{
    private final SelectPlaceService selectPlaceService;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    public SelectPlaceApplicationService(final SelectPlaceService selectPlaceService,
                                         final PlacesRepository placesRepository,
                                         final OrdersRepository ordersRepository)
    {
        this.selectPlaceService = selectPlaceService;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void selectPlaceFor(final PlaceId placeId, final OrderId orderId) throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException, AggregateRestoreException
    {
        // TODO: throw place not found exception
        final Place place = placesRepository.findById(placeId).orElseThrow();
        final Order order = ordersRepository.findById(orderId).orElseThrow();

        selectPlaceService.selectPlaceForOrder(place, order);

        placesRepository.save(place);
    }
}
