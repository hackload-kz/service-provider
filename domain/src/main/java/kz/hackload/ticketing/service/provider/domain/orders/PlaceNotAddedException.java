package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class PlaceNotAddedException extends Exception
{
    private final PlaceId placeId;

    public PlaceNotAddedException(final PlaceId placeId)
    {
        super("The place %s is not added".formatted(placeId));
        this.placeId = placeId;
    }

    public PlaceId placeId()
    {
        return placeId;
    }
}
