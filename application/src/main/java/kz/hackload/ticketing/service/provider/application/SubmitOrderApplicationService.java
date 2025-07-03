package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.*;

public final class SubmitOrderApplicationService implements SubmitOrderUseCase
{
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;

    public SubmitOrderApplicationService(final TransactionManager transactionManager,
                                         final OrdersRepository ordersRepository)
    {
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void submit(final OrderId orderId) throws OrderNotStartedException, NoPlacesAddedException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        order.submit();

        transactionManager.executeInTransaction(() -> ordersRepository.save(order));
    }
}
