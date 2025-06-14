package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public final class OrderStillContainsPlaceException extends Exception
{
    private final OrderId orderId;
    private final PlaceId placeId;

    public OrderStillContainsPlaceException(final OrderId orderId, final PlaceId placeId)
    {
        super("Order %s still contains place %s".formatted(orderId, placeId));
        this.orderId = orderId;
        this.placeId = placeId;
    }

    public OrderId orderId()
    {
        return orderId;
    }

    public PlaceId placeId()
    {
        return placeId;
    }
}
