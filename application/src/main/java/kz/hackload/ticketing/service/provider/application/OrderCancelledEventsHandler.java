package kz.hackload.ticketing.service.provider.application;

import kz.hackload.ticketing.service.provider.domain.orders.OrderCancelledEvent;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;

public final class OrderCancelledEventsHandler
{
    private final OrdersProjectionService ordersProjectionService;
    private final ReleasePlaceUseCase releasePlaceUseCase;

    public OrderCancelledEventsHandler(final OrdersProjectionService ordersProjectionService, final ReleasePlaceUseCase releasePlaceUseCase)
    {
        this.ordersProjectionService = ordersProjectionService;
        this.releasePlaceUseCase = releasePlaceUseCase;
    }

    public void handle(final OrderId id, final OrderCancelledEvent event) throws PlaceAlreadyReleasedException
    {
        ordersProjectionService.orderCancelled(id, event);
        releasePlaceUseCase.releasePlacesFromSingleOrder(event.placeId());
    }
}
