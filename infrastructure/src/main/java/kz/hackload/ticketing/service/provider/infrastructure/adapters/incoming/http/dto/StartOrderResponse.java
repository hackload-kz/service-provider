package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

@JsonSerialize(using = StartOrderResponseSerializer.class)
public record StartOrderResponse(OrderId orderId)
{
}
