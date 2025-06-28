package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

@JsonDeserialize(using = SubmitOrderDtoDeserializer.class)
public record SubmitOrderDto(OrderId orderId)
{
}
