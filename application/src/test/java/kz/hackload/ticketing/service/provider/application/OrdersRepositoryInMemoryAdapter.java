package kz.hackload.ticketing.service.provider.application;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class OrdersRepositoryInMemoryAdapter implements OrdersRepository
{
    private final Map<OrderId, Order> orders = new HashMap<>();

    @Override
    public OrderId nextId()
    {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public Optional<Order> findById(final OrderId id)
    {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public void save(final Order order)
    {
        orders.put(order.id(), order);
        order.commitEvents();
    }
}
