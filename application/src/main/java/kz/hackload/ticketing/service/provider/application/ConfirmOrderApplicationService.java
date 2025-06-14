package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotSubmittedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;

public final class ConfirmOrderApplicationService implements ConfirmOrderUseCase
{
    private final OrdersRepository ordersRepository;

    public ConfirmOrderApplicationService(final OrdersRepository ordersRepository)
    {
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void confirm(final OrderId orderId) throws OrderNotSubmittedException
    {
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        order.confirm();

        ordersRepository.save(order);
    }
}
