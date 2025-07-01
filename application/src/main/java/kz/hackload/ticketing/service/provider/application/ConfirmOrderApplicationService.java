package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotSubmittedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class ConfirmOrderApplicationService implements ConfirmOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;

    public ConfirmOrderApplicationService(final TransactionManager transactionManager,
                                          final OrdersRepository ordersRepository)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void confirm(final OrderId orderId) throws OrderNotSubmittedException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        order.confirm();

        transactionManager.executeInTransaction(() -> ordersRepository.save(order));
    }
}
