package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class StartOrderApplicationService implements StartOrderUseCase
{
    private final Clocks clocks;
    private final TransactionManager transactionManager;
    private final OrdersRepository ordersRepository;
    private final EventsDispatcher eventsDispatcher;

    public StartOrderApplicationService(final Clocks clocks,
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
    public OrderId startOrder()
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(clocks.now(), orderId);

        transactionManager.executeInTransaction(() ->
        {
            eventsDispatcher.dispatch(orderId, order.uncommittedEvents());
            ordersRepository.save(order);
        });

        return orderId;
    }
}
