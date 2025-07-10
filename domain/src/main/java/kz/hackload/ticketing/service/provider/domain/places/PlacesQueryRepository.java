package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Optional;

public interface PlacesQueryRepository
{
    Optional<GetPlaceQueryResult> getPlace(final PlaceId placeId);
}
