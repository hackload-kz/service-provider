package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

import org.jspecify.annotations.Nullable;

public record GetOrderQueryResult(OrderId id,
                                  OrderStatus status,
                                  long placesCount,
                                  Instant startedAt,
                                  Instant updatedAt,
                                  @Nullable
                                  Instant submittedAt)
{
}
