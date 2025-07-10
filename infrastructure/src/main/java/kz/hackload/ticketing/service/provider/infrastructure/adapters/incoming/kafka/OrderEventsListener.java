package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.OrdersProjectionService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderCancelledEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderConfirmedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderDomainEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStartedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderSubmittedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAddedToOrderEvent;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceRemovedFromOrderEvent;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class OrderEventsListener implements DomainEventsListener
{
    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final JsonMapper jsonMapper;
    private final OrdersProjectionService ordersProjectionService;
    private final ReleasePlaceUseCase releasePlaceUseCase;

    public OrderEventsListener(final JsonMapper jsonMapper, final OrdersProjectionService ordersProjectionService, final ReleasePlaceUseCase releasePlaceUseCase)
    {
        this.jsonMapper = jsonMapper;
        this.ordersProjectionService = ordersProjectionService;
        this.releasePlaceUseCase = releasePlaceUseCase;
    }

    @Override
    public String topic()
    {
        return "order-events";
    }

    @Override
    public void hande(final ConsumerRecord<String, String> record)
    {
        final OrderId orderId = new OrderId(UUID.fromString(record.key()));
        final String value = record.value();

        final Class<?> eventType = mapToOrderDomainEventClass(
                new String(record.headers().lastHeader("event_type").value(), StandardCharsets.UTF_8)
        );

        final OrderDomainEvent event = (OrderDomainEvent) jsonMapper.fromJson(value, eventType);

        switch (event)
        {
            case OrderStartedEvent e -> ordersProjectionService.orderStarted(orderId, e);
            case PlaceAddedToOrderEvent e -> ordersProjectionService.placeAddedToOrder(orderId, e);
            case PlaceRemovedFromOrderEvent e -> {
                releasePlace(e);
                ordersProjectionService.placeRemovedFromOrder(orderId, e);
            }
            case OrderSubmittedEvent e -> ordersProjectionService.orderSubmitted(orderId, e);
            case OrderConfirmedEvent e -> ordersProjectionService.orderConfirmed(orderId, e);
            case OrderCancelledEvent e ->
            {
            }
        }
    }

    private Class<? extends OrderDomainEvent> mapToOrderDomainEventClass(final String eventType)
    {
        return switch (eventType)
        {
            case "order_started_event" -> OrderStartedEvent.class;
            case "order_cancelled_event" -> OrderCancelledEvent.class;
            case "order_confirmed_event" -> OrderConfirmedEvent.class;
            case "order_submitted_event" -> OrderSubmittedEvent.class;
            case "place_added_to_order_event" -> PlaceAddedToOrderEvent.class;
            case "place_removed_from_order_event" -> PlaceRemovedFromOrderEvent.class;
            default -> throw new IllegalArgumentException("Unexpected value: " + eventType);
        };
    }

    private void releasePlace(final PlaceRemovedFromOrderEvent event)
    {
        final PlaceId placeId = event.placeId();

        try
        {
            releasePlaceUseCase.releasePlace(placeId);
        }
        catch (final OrderNotStartedException
                     | PlaceSelectedForAnotherOrderException
                     | PlaceNotAddedException
                     | PlaceAlreadyReleasedException e)
        {
            log.error(e.getMessage(), e);
        }
    }
}
