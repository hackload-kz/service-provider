package kz.hackload.ticketing.service.provider.application;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceDomainEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public class InMemoryPlacesRepository implements PlacesRepository
{
    private final ConcurrentMap<PlaceId, ReentrantLock> locks = new ConcurrentHashMap<>();
    private final ConcurrentMap<PlaceId, List<PlaceDomainEvent>> eventsStore = new ConcurrentHashMap<>();
    private final ConcurrentMap<PlaceId, Long> versionsStore = new ConcurrentHashMap<>();

    @Override
    public void save(final Place place)
    {
        final ReentrantLock lock = locks.computeIfAbsent(place.id(), _ -> new ReentrantLock());

        lock.lock();
        try
        {
            final PlaceId placeId = place.id();

            final Long currentVersion = versionsStore.get(place.id());
            final long expectedRevision = place.revision();

            if (currentVersion != null && currentVersion != expectedRevision)
            {
                throw new ConcurrentModificationException("Revision mismatch exception for %s".formatted(placeId));
            }

            final List<PlaceDomainEvent> uncommittedEvents = place.uncommittedEvents();

            final List<PlaceDomainEvent> events = eventsStore.get(placeId);
            if (events == null)
            {
                eventsStore.put(placeId, new ArrayList<>(uncommittedEvents));
                versionsStore.put(placeId, (long) uncommittedEvents.size());
            }
            else
            {
                for (final PlaceDomainEvent uncommittedEvent : uncommittedEvents)
                {
                    events.add(uncommittedEvent);
                    versionsStore.compute(placeId, (k, v) -> v == null ? 1 : v + 1);
                }
            }

            place.commitEvents();
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public Optional<Place> findById(final PlaceId placeId)
    {
        final Long revision = versionsStore.get(placeId);

        if (revision == null)
        {
            return Optional.empty();
        }

        final List<PlaceDomainEvent> events = eventsStore.get(placeId);

        final Place place;
        try
        {
            place = Place.restore(placeId, revision, events);
        }
        catch (final AggregateRestoreException e)
        {
            throw new RuntimeException(e);
        }

        return Optional.of(place);
    }
}
