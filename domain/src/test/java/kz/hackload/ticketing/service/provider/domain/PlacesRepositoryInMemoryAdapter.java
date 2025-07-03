package kz.hackload.ticketing.service.provider.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class PlacesRepositoryInMemoryAdapter implements PlacesRepository
{
    private final Map<PlaceId, Place> places = new HashMap<>();

    @Override
    public PlaceId nextId()
    {
        return new PlaceId(UUID.randomUUID());
    }

    @Override
    public void save(final Place place)
    {
        places.put(place.id(), place);
        place.commitEvents();
    }

    @Override
    public Optional<Place> findById(final PlaceId placeId)
    {
        return Optional.ofNullable(places.get(placeId));
    }
}
