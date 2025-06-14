package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotSubmittedException;

public interface ConfirmOrderUseCase
{
    void confirm(final OrderId orderId) throws OrderNotSubmittedException;
}
