package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceNotAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceRemovedFromOrderEvent;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class OrderEventsListener implements DomainEventsListener
{
    private static final Logger log = LoggerFactory.getLogger(OrderEventsListener.class);

    private final JsonMapper jsonMapper;
    private final ReleasePlaceUseCase releasePlaceUseCase;

    public OrderEventsListener(final JsonMapper jsonMapper, final ReleasePlaceUseCase releasePlaceUseCase)
    {
        this.jsonMapper = jsonMapper;
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
        final String value = record.value();
        final PlaceRemovedFromOrderEvent event = jsonMapper.fromJson(value, PlaceRemovedFromOrderEvent.class);

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
