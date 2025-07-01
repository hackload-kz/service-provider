package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderAlreadyCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class CancelOrderApplicationService implements CancelOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;

    public CancelOrderApplicationService(final TransactionManager transactionManager,
                                         final OrdersRepository ordersRepository)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void cancel(final OrderId orderId) throws OrderAlreadyCancelledException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        order.cancel();

        transactionManager.executeInTransaction(() -> ordersRepository.save(order));
    }
}
