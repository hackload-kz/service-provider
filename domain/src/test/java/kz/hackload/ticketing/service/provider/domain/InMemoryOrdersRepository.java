package kz.hackload.ticketing.service.provider.domain;

import java.util.Optional;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public class InMemoryOrdersRepository implements OrdersRepository
{
    @Override
    public OrderId nextId()
    {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public Optional<Order> findById(final OrderId id)
    {
        return Optional.empty();
    }

    @Override
    public void save(final Order order)
    {

    }
}
