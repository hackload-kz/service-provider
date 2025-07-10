package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.Optional;

public interface OrdersQueryRepository
{
    Optional<GetOrderQueryResult> getOrder(final OrderId orderId);
}
