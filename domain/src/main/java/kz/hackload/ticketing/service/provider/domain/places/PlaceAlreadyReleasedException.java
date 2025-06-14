package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Objects;

public final class PlaceAlreadyReleasedException extends Exception
{
    private final PlaceId placeId;

    public PlaceAlreadyReleasedException(final PlaceId placeId)
    {
        super("The place %s is already released".formatted(placeId));
        this.placeId = Objects.requireNonNull(placeId);
    }

    public PlaceId placeId()
    {
        return placeId;
    }
}
