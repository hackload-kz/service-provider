package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public final class SelectPlaceApplicationService implements SelectPlaceUseCase
{
    private final SelectPlaceService selectPlaceService;

    private final TransactionManager transactionManager;
    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    public SelectPlaceApplicationService(final SelectPlaceService selectPlaceService,
                                         final TransactionManager transactionManager,
                                         final PlacesRepository placesRepository,
                                         final OrdersRepository ordersRepository)
    {
        this.selectPlaceService = selectPlaceService;
        this.transactionManager = transactionManager;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void selectPlaceFor(final PlaceId placeId, final OrderId orderId) throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException, AggregateRestoreException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        selectPlaceService.selectPlaceForOrder(place, order);

        transactionManager.executeInTransaction(() -> placesRepository.save(place));
    }
}
