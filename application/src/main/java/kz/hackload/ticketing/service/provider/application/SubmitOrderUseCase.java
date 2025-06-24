package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;

public interface SubmitOrderUseCase
{
    void submit(final OrderId orderId) throws OrderNotStartedException, NoPlacesAddedException, AggregateRestoreException;
}
