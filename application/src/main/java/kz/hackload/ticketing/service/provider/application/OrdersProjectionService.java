package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderCancelledEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderConfirmedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStartedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderSubmittedEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersProjectionsRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAddedToOrderEvent;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceRemovedFromOrderEvent;

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

    public void placeAddedToOrder(final OrderId orderId, final PlaceAddedToOrderEvent e)
    {
        ordersQueryRepository.incrementPlacesCount(orderId, e.occurredOn());
    }

    public void placeRemovedFromOrder(final OrderId orderId, final PlaceRemovedFromOrderEvent e)
    {
        ordersQueryRepository.decrementPlacesCount(orderId, e.occurredOn());
    }

    public void orderSubmitted(final OrderId orderId, final OrderSubmittedEvent e)
    {
        ordersQueryRepository.orderSubmitted(orderId, e.occurredOn());
    }

    public void orderConfirmed(final OrderId orderId, final OrderConfirmedEvent e)
    {
        ordersQueryRepository.orderConfirmed(orderId, e.occurredOn());
    }

    public void orderCancelled(final OrderId orderId, final OrderCancelledEvent e)
    {
        ordersQueryRepository.orderCancelled(orderId, e.occurredOn());
    }
}
