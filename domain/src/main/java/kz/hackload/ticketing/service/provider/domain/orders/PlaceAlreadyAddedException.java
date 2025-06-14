package kz.hackload.ticketing.service.provider.domain.orders;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class PlaceAlreadyAddedException extends Exception
{
    private final PlaceId placeId;

    public PlaceAlreadyAddedException(final PlaceId placeId)
    {
        super("The place %s is already added".formatted(placeId));
        this.placeId = placeId;
    }

    public PlaceId placeId()
    {
        return placeId;
    }
}
