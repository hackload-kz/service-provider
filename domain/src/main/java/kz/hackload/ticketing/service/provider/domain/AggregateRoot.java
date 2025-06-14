package kz.hackload.ticketing.service.provider.domain;
 
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import kz.hackload.ticketing.service.provider.domain.places.Place;

public abstract class AggregateRoot<ID extends DomainEntityId, EVENT extends DomainEvent>
{
    protected final ID id;
    private final List<EVENT> events = new ArrayList<>();
    private long revision;

    protected AggregateRoot(final ID id)
    {
        this.id = Objects.requireNonNull(id);
        this.revision = 0;
    }

    public final ID id()
    {
        return id;
    }

    public final List<EVENT> uncommittedEvents()
    {
        return List.copyOf(events);
    }

    public final void commitEvents()
    {
        events.clear();
    }

    public final long revision()
    {
        return revision;
    }

    protected final void setRevision(final long revision)
    {
        this.revision = revision;
    }

    protected final void addEvent(final EVENT event)
    {
        events.add(event);
    }

    @Override
    public final boolean equals(final Object thatObject)
    {
        if (thatObject == null || getClass() != thatObject.getClass())
        {
            return false;
        }

        final Place that = (Place) thatObject;

        return Objects.equals(this.id(), that.id());
    }

    @Override
    public final int hashCode()
    {
        return Objects.hashCode(id());
    }
}
