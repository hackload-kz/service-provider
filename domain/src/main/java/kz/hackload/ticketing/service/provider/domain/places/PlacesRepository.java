package kz.hackload.ticketing.service.provider.domain.places;

import java.util.Optional;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;

public interface PlacesRepository
{
    PlaceId nextId();

    void save(Place place);

    Optional<Place> findById(PlaceId placeId) throws AggregateRestoreException;
}
