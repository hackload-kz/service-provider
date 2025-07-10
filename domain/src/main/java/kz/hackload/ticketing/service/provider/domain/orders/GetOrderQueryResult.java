package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

public record GetOrderQueryResult(OrderId id, OrderStatus status, long placesCount, Instant startedAt, Instant updatedAt)
{
}
