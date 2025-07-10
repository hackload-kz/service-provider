package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.orders.GetOrderQueryResult;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersQueryRepository;

public final class OrdersQueryService implements GetOrderUseCase
{
    private final OrdersQueryRepository ordersRepository;

    public OrdersQueryService(final OrdersQueryRepository ordersRepository)
    {
        this.ordersRepository = ordersRepository;
    }

    @Override
    public Optional<GetOrderQueryResult> getOrder(final OrderId orderId)
    {
        return ordersRepository.getOrder(orderId);
    }
}
