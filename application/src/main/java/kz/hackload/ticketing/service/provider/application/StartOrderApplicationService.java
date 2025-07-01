package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class StartOrderApplicationService implements StartOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;

    public StartOrderApplicationService(final TransactionManager transactionManager, final OrdersRepository ordersRepository)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public OrderId startOrder()
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        return orderId;
    }
}
