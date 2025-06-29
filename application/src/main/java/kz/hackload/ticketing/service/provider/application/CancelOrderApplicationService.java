package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderAlreadyCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class CancelOrderApplicationService implements CancelOrderUseCase
{
    private final OrdersRepository ordersRepository;

    public CancelOrderApplicationService(final OrdersRepository ordersRepository)
    {
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void cancel(final OrderId orderId) throws AggregateRestoreException, OrderAlreadyCancelledException
    {
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        order.cancel();
        ordersRepository.save(order);
    }
}
