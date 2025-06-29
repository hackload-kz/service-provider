package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderAlreadyCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public interface CancelOrderUseCase
{
    void cancel(final OrderId orderId) throws AggregateRestoreException, OrderAlreadyCancelledException;
}
