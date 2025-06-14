package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class PlaceSelectedForAnotherOrderException extends Exception
{
    private final PlaceId placeId;
    private final OrderId selectedForOrder;
    private final OrderId augendOrder;

    public PlaceSelectedForAnotherOrderException(final PlaceId placeId, final OrderId selectedForOrder, final OrderId augendOrder)
    {
        super("Place %s is selected for order %s not for order %s".formatted(placeId, selectedForOrder, augendOrder));
        this.placeId = placeId;
        this.selectedForOrder = selectedForOrder;
        this.augendOrder = augendOrder;
    }

    public PlaceId placeId()
    {
        return placeId;
    }

    public OrderId selectedForOrder()
    {
        return selectedForOrder;
    }

    public OrderId augendOrder()
    {
        return augendOrder;
    }
}
