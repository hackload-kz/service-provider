package kz.hackload.ticketing.service.provider.application;

import java.util.List;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class RemovePlaceFromOrderFromOrderApplicationService implements RemovePlaceFromOrderUseCase
{
    private final JsonMapper jsonMapper;
    private final TransactionManager transactionManager;

    private final OutboxRepository outboxRepository;
    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;

    private final RemovePlaceFromOrderService removePlaceFromOrderService;

    public RemovePlaceFromOrderFromOrderApplicationService(final JsonMapper jsonMapper,
                                                           final TransactionManager transactionManager,
                                                           final OutboxRepository outboxRepository,
                                                           final PlacesRepository placesRepository,
                                                           final OrdersRepository ordersRepository,
                                                           final RemovePlaceFromOrderService removePlaceFromOrderService)
    {
        this.jsonMapper = jsonMapper;
        this.transactionManager = transactionManager;
        this.outboxRepository = outboxRepository;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
        this.removePlaceFromOrderService = removePlaceFromOrderService;
    }

    @Override
    public void removePlaceFromOrder(final PlaceId placeId) throws OrderNotStartedException, PlaceNotAddedException, PlaceSelectedForAnotherOrderException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final OrderId orderId = place.selectedFor().orElseThrow();
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        removePlaceFromOrderService.removePlace(order, place);

        final List<OutboxMessage> outboxMessages = order.uncommittedEvents()
                .stream()
                .map(e -> new OutboxMessage(outboxRepository.nextId(), "order-events", orderId.value().toString(),  e.revision(), "order", jsonMapper.toJson(e)))
                .toList();

        transactionManager.executeInTransaction(() ->
        {
            ordersRepository.save(order);
            outboxMessages.forEach(outboxRepository::save);
        });
    }
}
