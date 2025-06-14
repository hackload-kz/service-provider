package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public interface StartOrderUseCase
{
    OrderId startOrder();
}
