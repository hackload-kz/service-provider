package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Objects;

public final class PlaceAlreadySelectedException extends Exception
{
    private final PlaceId placeId;

    public PlaceAlreadySelectedException(final PlaceId placeId)
    {
        super("The place %s is already selected".formatted(Objects.requireNonNull(placeId)));
        this.placeId = Objects.requireNonNull(placeId);
    }

    public PlaceId placeId()
    {
        return placeId;
    }
}
