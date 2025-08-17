package kz.hackload.ticketing.service.provider.application;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger LOG = LoggerFactory.getLogger(ReleasePlaceApplicationService.class);

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

    @Override
    public void releasePlacesFromSingleOrder(final Set<PlaceId> placeIds) throws PlaceAlreadyReleasedException
    {
        if (placeIds.isEmpty())
        {
            return;
        }

        final List<Place> places = transactionManager.executeInTransaction(() -> placesRepository.findAll(placeIds));

        final List<OrderId> orderIds = places.stream()
                .map(Place::selectedFor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();

        if (orderIds.size() != 1)
        {
            LOG.warn("Something is wrong. Places: {} are selected to Orders: {}", placeIds, orderIds);
            return;
        }

        final OrderId orderId = orderIds.getFirst();
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        for (final Place place : places)
        {
            releasePlaceService.release(order, place);
        }

        transactionManager.executeInTransaction(() ->
        {
            places.forEach(place -> eventsDispatcher.dispatch(place.id(), place.uncommittedEvents()));
            places.forEach(placesRepository::save);
        });
    }
}
