package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.orders.ConfirmedOrderCanNotBeCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderAlreadyCancelledException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class CancelOrderApplicationService implements CancelOrderUseCase
{
    private final Clocks clocks;
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;
    private final EventsDispatcher eventsDispatcher;

    public CancelOrderApplicationService(final Clocks clocks,
                                         final TransactionManager transactionManager,
                                         final OrdersRepository ordersRepository,
                                         final EventsDispatcher eventsDispatcher)
    {
        this.clocks = clocks;
        this.transactionManager = transactionManager;
        this.ordersRepository = ordersRepository;
        this.eventsDispatcher = eventsDispatcher;
    }

    @Override
    public void cancel(final OrderId orderId) throws OrderAlreadyCancelledException,
            ConfirmedOrderCanNotBeCancelledException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        order.cancel(clocks.now());

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(orderId, order.uncommittedEvents());
            ordersRepository.save(order);
        });
    }
}
