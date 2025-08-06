package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.PlacesProjectionService;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCreatedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceDomainEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceReleasedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceSelectedEvent;

public final class PlaceEventsListener implements DomainEventsListener
{
    private static final Logger LOG = LoggerFactory.getLogger(PlaceEventsListener.class);

    private final JsonMapper jsonMapper;
    private final PlacesProjectionService placesProjectionService;
    private final AddPlaceToOrderUseCase addPlaceToOrderUseCase;

    private volatile boolean running;

    public PlaceEventsListener(final JsonMapper jsonMapper,
                               final PlacesProjectionService placesProjectionService,
                               final AddPlaceToOrderUseCase addPlaceToOrderUseCase)
    {
        this.jsonMapper = jsonMapper;
        this.placesProjectionService = placesProjectionService;
        this.addPlaceToOrderUseCase = addPlaceToOrderUseCase;
    }

    @Override
    public String topic()
    {
        return "place-events";
    }

    @Override
    public void hande(final ConsumerRecord<String, String> record)
    {
        final String value = record.value();

        final Class<? extends PlaceDomainEvent> eventType = mapToPlaceDomainEventType(
                new String(record.headers().lastHeader("event_type").value(), StandardCharsets.UTF_8)
        );

        final PlaceId placeId = new PlaceId(UUID.fromString(record.key()));
        final PlaceDomainEvent event = jsonMapper.fromJson(value, eventType);

        switch (event)
        {
            case PlaceCreatedEvent e -> placesProjectionService.placeCreated(placeId, e);
            case PlaceSelectedEvent e ->
            {
                placesProjectionService.placeSelected(placeId, e);
                addPlaceToOrder(placeId, e);
            }
            case PlaceReleasedEvent e -> placesProjectionService.placeReleased(placeId, e);
        }
    }

    private Class<? extends PlaceDomainEvent> mapToPlaceDomainEventType(final String eventType)
    {
        return switch (eventType)
        {
            case "place_created_event" -> PlaceCreatedEvent.class;
            case "place_selected_event" -> PlaceSelectedEvent.class;
            case "place_released_event" -> PlaceReleasedEvent.class;
            default -> throw new IllegalArgumentException("Unexpected value: " + eventType);
        };
    }

    private void addPlaceToOrder(final PlaceId placeId, final PlaceSelectedEvent event)
    {
        final OrderId orderId = event.orderId();

        try
        {
            addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);
        }
        catch (final OrderNotStartedException
                     | PlaceIsNotSelectedException
                     | PlaceSelectedForAnotherOrderException
                     | PlaceAlreadyAddedException e)
        {
            throw new RuntimeException(e);
        }
    }
}
