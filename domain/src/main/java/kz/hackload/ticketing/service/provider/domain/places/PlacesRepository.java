package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PlacesRepository
{
    PlaceId nextId();

    void save(Place place);

    Optional<Place> findById(PlaceId placeId);

    List<Place> findAll(final Collection<PlaceId> placeIds);
}
