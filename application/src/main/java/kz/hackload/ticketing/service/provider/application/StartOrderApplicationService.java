package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class StartOrderApplicationService implements StartOrderUseCase
{
    private final OrdersRepository ordersRepository;

    public StartOrderApplicationService(final OrdersRepository ordersRepository)
    {
        this.ordersRepository = ordersRepository;
    }

    @Override
    public OrderId startOrder()
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        ordersRepository.save(order);

        return orderId;
    }
}
