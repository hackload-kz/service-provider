package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.orders.GetOrderQueryResult;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public interface GetOrderUseCase
{
    Optional<GetOrderQueryResult> order(final OrderId orderId);
}
