package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Optional;

public interface PlacesRepository
{
    void save(Place place);

    Optional<Place> findById(PlaceId placeId);
}
