package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;

public interface OrdersProjectionsRepository
{
    void insertStartedOrder(final OrderId orderId, final Instant startedAt, final long revision);

    void incrementPlacesCount(final OrderId orderId, final Instant placeAddedAt);

    void decrementPlacesCount(final OrderId orderId, final Instant placeRemovedAt);
}
