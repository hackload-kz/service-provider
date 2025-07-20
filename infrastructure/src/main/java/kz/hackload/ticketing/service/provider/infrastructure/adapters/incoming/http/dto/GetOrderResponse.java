package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.time.Instant;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;

@JsonSerialize(using = GetOrderResponseSerializer.class)
public record GetOrderResponse(OrderId id,
                               OrderStatus orderStatus,
                               Instant startedAt,
                               Instant updatedAt,
                               long placesCount)
{
}
