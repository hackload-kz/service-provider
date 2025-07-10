package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStartedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersProjectionsRepository;

public class OrdersProjectionService
{
    private final OrdersProjectionsRepository ordersQueryRepository;

    public OrdersProjectionService(final OrdersProjectionsRepository ordersQueryRepository)
    {
        this.ordersQueryRepository = ordersQueryRepository;
    }

    public void orderStarted(final OrderId orderId, final OrderStartedEvent event)
    {
        ordersQueryRepository.insertStartedOrder(orderId, event.occurredOn(), event.revision());
    }
}
