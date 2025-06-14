package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.*;

public final class SubmitOrderApplicationService implements SubmitOrderUseCase
{
    private final OrdersRepository ordersRepository;

    public SubmitOrderApplicationService(final OrdersRepository ordersRepository)
    {
        this.ordersRepository = ordersRepository;
    }

    @Override
    public void submit(final OrderId orderId) throws OrderNotStartedException, NoPlacesAddedException
    {
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        order.submit();

        ordersRepository.save(order);
    }
}
