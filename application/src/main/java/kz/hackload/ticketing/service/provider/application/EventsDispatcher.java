package kz.hackload.ticketing.service.provider.application;

import java.util.List;

import kz.hackload.ticketing.service.provider.domain.DomainEntityId;
import kz.hackload.ticketing.service.provider.domain.DomainEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderDomainEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlaceDomainEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class EventsDispatcher
{
    private final JsonMapper jsonMapper;
    private final OutboxRepository outboxRepository;

    public EventsDispatcher(final JsonMapper jsonMapper, final OutboxRepository outboxRepository)
    {
        this.jsonMapper = jsonMapper;
        this.outboxRepository = outboxRepository;
    }

    public void dispatch(final OrderId orderId, final List<? extends OrderDomainEvent> domainEvents)
    {
        dispatch("order-events", orderId, "order", domainEvents);
    }

    public void dispatch(final PlaceId placeId, final List<? extends PlaceDomainEvent> domainEvents)
    {
        dispatch("place-events", placeId, "place", domainEvents);
    }

    private void dispatch(final String topic,
                          final DomainEntityId<?> aggregateId,
                          final String aggregateType,
                          final List<? extends DomainEvent> domainEvents)
    {
        final List<OutboxMessage> outboxMessages = domainEvents
                .stream()
                .map(event -> new OutboxMessage(outboxRepository.nextId(), topic, aggregateId.toString(), event.occurredOn(), aggregateType, event.type(), jsonMapper.toJson(event)))
                .toList();

        outboxMessages.forEach(outboxRepository::save);
    }
}
