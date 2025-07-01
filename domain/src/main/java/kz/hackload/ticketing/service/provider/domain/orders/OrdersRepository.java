package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.Optional;

public interface OrdersRepository
{
    OrderId nextId();

    Optional<Order> findById(final OrderId id);

    void save(final Order order);
}
