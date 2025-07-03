package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

@JsonDeserialize(using = SelectPlaceDtoDeserializer.class)
public record SelectPlaceDto(
        OrderId orderId,
        PlaceId placeId)
{
}
