package kz.hackload.ticketing.service.provider.domain.places;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public final class PlaceCanNotBeAddedToOrderException extends Exception
{
    private final PlaceId placeId;
    private final OrderId orderId;

    public PlaceCanNotBeAddedToOrderException(final PlaceId placeId, final OrderId orderId)
    {
        super("Place %s can not be added to order %s".formatted(placeId, orderId));
        this.placeId = placeId;
        this.orderId = orderId;
    }

    public PlaceId placeId()
    {
        return placeId;
    }

    public OrderId orderId()
    {
        return orderId;
    }
}
