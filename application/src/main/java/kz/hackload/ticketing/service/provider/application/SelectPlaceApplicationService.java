package kz.hackload.ticketing.service.provider.application;

import java.util.List;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessageId;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
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
    private final JsonMapper jsonMapper;

    private final PlacesRepository placesRepository;
    private final OrdersRepository ordersRepository;
    private final OutboxRepository outboxRepository;

    public SelectPlaceApplicationService(final SelectPlaceService selectPlaceService,
                                         final TransactionManager transactionManager,
                                         final JsonMapper jsonMapper,
                                         final PlacesRepository placesRepository,
                                         final OrdersRepository ordersRepository,
                                         final OutboxRepository outboxRepository)
    {
        this.selectPlaceService = selectPlaceService;
        this.transactionManager = transactionManager;
        this.jsonMapper = jsonMapper;
        this.placesRepository = placesRepository;
        this.ordersRepository = ordersRepository;
        this.outboxRepository = outboxRepository;
    }

    @Override
    public void selectPlaceFor(final PlaceId placeId, final OrderId orderId) throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException
    {
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        selectPlaceService.selectPlaceForOrder(place, order);

        final OutboxMessageId outboxMessageId = outboxRepository.nextId();

        final List<OutboxMessage> outboxMessages = place.uncommittedEvents()
                .stream()
                .map(event -> new OutboxMessage(outboxMessageId, "place-events", placeId.value().toString(), "place", jsonMapper.toJson(event)))
                .toList();

        transactionManager.executeInTransaction(() ->
        {
            placesRepository.save(place);
            outboxMessages.forEach(outboxRepository::save);
        });
    }
}
