package kz.hackload.ticketing.service.provider.domain.places;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.Nullable;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.AggregateRoot;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public final class Place extends AggregateRoot<PlaceId, PlaceDomainEvent>
{
    private final Row row;
    private final Seat seat;
    private boolean selected;
    @Nullable
    private OrderId selectedFor;

    private Place(final PlaceId id, final Row row, final Seat seat)
    {
        super(id);
        this.row = row;
        this.seat = seat;
    }

    public static Place create(final Instant createdAt, final PlaceId placeId, final Row row, final Seat seat)
    {
        final Place place = new Place(placeId, row, seat);
        final long revision = place.incrementRevision();

        final PlaceCreatedEvent event = new PlaceCreatedEvent(createdAt, revision, row, seat);
        place.apply(event);
        place.addEvent(event);

        return place;
    }

    public static Place restore(final PlaceId id, final List<PlaceDomainEvent> events)
    {
        final PlaceCreatedEvent placeCreatedEvent = (PlaceCreatedEvent) events.getFirst();
        final Place place = new Place(id, placeCreatedEvent.row(), placeCreatedEvent.seat());
        for (final PlaceDomainEvent event : events)
        {
            try
            {
                place.apply(event);
            }
            catch (PlaceAlreadySelectedException | PlaceAlreadyReleasedException e)
            {
                throw new AggregateRestoreException(e);
            }
        }

        return place;
    }

    public Row row()
    {
        return row;
    }

    public Seat seat()
    {
        return seat;
    }

    public void selectFor(final Instant selectedAt, final OrderId orderId) throws PlaceAlreadySelectedException
    {
        final long revision = incrementRevision();
        final PlaceSelectedEvent event = new PlaceSelectedEvent(selectedAt, revision, orderId);
        apply(event);
        addEvent(event);
    }

    public Optional<OrderId> selectedFor()
    {
        return Optional.ofNullable(selectedFor);
    }

    public boolean isSelectedFor(final OrderId orderId)
    {
        return orderId.equals(selectedFor);
    }

    public void release(final Instant releasedAt) throws PlaceAlreadyReleasedException
    {
        final long revision = incrementRevision();
        final PlaceReleasedEvent placeReleasedEvent = new PlaceReleasedEvent(releasedAt, revision, selectedFor);
        apply(placeReleasedEvent);
        addEvent(placeReleasedEvent);
    }

    public boolean isFree()
    {
        return !selected;
    }

    private void apply(final PlaceDomainEvent event) throws PlaceAlreadySelectedException,
            PlaceAlreadyReleasedException
    {
        switch (event)
        {
            case PlaceSelectedEvent e -> apply(e);
            case PlaceReleasedEvent e -> apply(e);
            case PlaceCreatedEvent e -> apply(e);
        }

        setRevision(event.revision());
    }

    private void apply(final PlaceSelectedEvent event) throws PlaceAlreadySelectedException
    {
        if (!isFree())
        {
            throw new PlaceAlreadySelectedException(id);
        }

        final OrderId orderId = event.orderId();

        selected = true;
        selectedFor = orderId;
    }

    private void apply(final PlaceReleasedEvent event) throws PlaceAlreadyReleasedException
    {
        if (isFree())
        {
            throw new PlaceAlreadyReleasedException(id);
        }

        selected = false;
        selectedFor = null;
    }

    private void apply(final PlaceCreatedEvent event)
    {
        selected = false;
        selectedFor = null;
    }
}
