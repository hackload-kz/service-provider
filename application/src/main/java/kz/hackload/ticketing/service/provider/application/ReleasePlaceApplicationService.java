package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class ReleasePlaceApplicationService implements ReleasePlaceUseCase
{
    private final ReleasePlaceService releasePlaceService;

    private final TransactionManager transactionManager;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    public ReleasePlaceApplicationService(final ReleasePlaceService releasePlaceService,
                                          final TransactionManager transactionManager,
                                          final PlacesRepository placesRepository,
                                          final OrdersRepository ordersRepository)
    {
        this.releasePlaceService = releasePlaceService;
        this.transactionManager = transactionManager;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void releasePlace(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final OrderId orderId = place.selectedFor().orElseThrow();
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        releasePlaceService.release(order, place);

        transactionManager.executeInTransaction(() -> ordersRepository.save(order));
    }
}
