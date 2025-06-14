package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class PlaceIsNotSelectedException extends Exception
{
    private final PlaceId placeId;
    private final OrderId orderId;

    public PlaceIsNotSelectedException(final PlaceId placeId, final OrderId orderId)
    {
        super("Place %s is not selected and can not be added to order %s".formatted(placeId, orderId));
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
