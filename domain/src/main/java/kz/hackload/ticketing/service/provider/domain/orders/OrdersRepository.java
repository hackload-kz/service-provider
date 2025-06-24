package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;

public interface OrdersRepository
{
    OrderId nextId();

    Optional<Order> findById(final OrderId id) throws AggregateRestoreException;

    void save(final Order order);
}
