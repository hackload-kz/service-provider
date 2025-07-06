package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceSelectedEvent;

public final class PlaceEventsListener implements DomainEventsListener
{
    private final JsonMapper jsonMapper;
    private final AddPlaceToOrderUseCase addPlaceToOrderUseCase;

    private volatile boolean running;

    public PlaceEventsListener(final JsonMapper jsonMapper,
                               final AddPlaceToOrderUseCase addPlaceToOrderUseCase)
    {
        this.jsonMapper = jsonMapper;
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
        final PlaceSelectedEvent event = jsonMapper.fromJson(value, PlaceSelectedEvent.class);

        final OrderId orderId = event.orderId();
        final PlaceId placeId = new PlaceId(UUID.fromString(record.key()));

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
