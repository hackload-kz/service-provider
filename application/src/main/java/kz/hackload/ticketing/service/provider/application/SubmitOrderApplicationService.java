package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class SubmitOrderApplicationService implements SubmitOrderUseCase
{
    private final Clocks clocks;
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;
    private final EventsDispatcher eventsDispatcher;

    public SubmitOrderApplicationService(final Clocks clocks,
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
    public void submit(final OrderId orderId) throws OrderNotStartedException, NoPlacesAddedException
    {
        final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        order.submit(clocks.now());

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(orderId, order.uncommittedEvents());
            ordersRepository.save(order);
        });
    }
}
