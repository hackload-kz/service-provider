package kz.hackload.ticketing.service.provider.domain.orders;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.AggregateRoot;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public final class Order extends AggregateRoot<OrderId, OrderDomainEvent>
{
    private final Set<PlaceId> places;
    private OrderStatus status;

    private Order(final OrderId orderId)
    {
        super(orderId);

        this.places = new HashSet<>();
        this.status = OrderStatus.STARTED;
    }

    public static Order start(final Instant startedAt, final OrderId orderId)
    {
        final Order order = new Order(orderId);
        final long revision = order.incrementRevision();

        final OrderStartedEvent event = new OrderStartedEvent(startedAt, revision);

        order.addEvent(event);

        return order;
    }

    public static Order restore(final OrderId id, final List<OrderDomainEvent> events)
    {
        final Order order = new Order(id);
        for (OrderDomainEvent event : events)
        {
            try
            {
                order.apply(event);
            }
            catch (final PlaceNotAddedException | PlaceAlreadyAddedException | NoPlacesAddedException |
                         OrderNotStartedException | OrderNotSubmittedException | OrderAlreadyCancelledException |
                         ConfirmedOrderCanNotBeCancelledException e)
            {
                throw new AggregateRestoreException(e);
            }
        }

        return order;
    }

    public OrderStatus status()
    {
        return status;
    }

    public Set<PlaceId> places()
    {
        return Set.copyOf(places);
    }

    public boolean contains(final PlaceId placeId)
    {
        return places.contains(placeId);
    }

    public boolean canAddPlace()
    {
        return OrderStatus.STARTED == status;
    }

    public void addPlace(final Instant addedAt, final PlaceId placeId) throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final long revision = incrementRevision();
        final PlaceAddedToOrderEvent event = new PlaceAddedToOrderEvent(addedAt, revision, placeId);

        apply(event);
        addEvent(event);
    }

    public void removePlace(final Instant removedAt, final PlaceId placeId) throws PlaceNotAddedException, OrderNotStartedException
    {
        final long revision = incrementRevision();
        final PlaceRemovedFromOrderEvent event = new PlaceRemovedFromOrderEvent(removedAt, revision, placeId);
        apply(event);
        addEvent(event);
    }

    public void submit(final Instant submittedAt) throws NoPlacesAddedException, OrderNotStartedException
    {
        final long revision = incrementRevision();
        final OrderSubmittedEvent event = new OrderSubmittedEvent(submittedAt, revision);
        apply(event);
        addEvent(event);
    }

    public void confirm(final Instant confirmedAt) throws OrderNotSubmittedException
    {
        final long revision = incrementRevision();
        final OrderConfirmedEvent orderConfirmedEvent = new OrderConfirmedEvent(confirmedAt, revision);
        apply(orderConfirmedEvent);
        addEvent(orderConfirmedEvent);
    }

    public void cancel(final Instant cancelledAt) throws OrderAlreadyCancelledException,
            ConfirmedOrderCanNotBeCancelledException
    {
        final long revision = incrementRevision();
        final OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent(cancelledAt, revision, places());
        apply(orderCancelledEvent);
        addEvent(orderCancelledEvent);
    }

    private void apply(final OrderDomainEvent orderDomainEvent) throws PlaceNotAddedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException,
            OrderNotStartedException,
            OrderNotSubmittedException,
            OrderAlreadyCancelledException,
            ConfirmedOrderCanNotBeCancelledException
    {
        switch (orderDomainEvent)
        {
            case OrderStartedEvent e -> apply(e);
            case OrderSubmittedEvent e -> apply(e);
            case PlaceAddedToOrderEvent e -> apply(e);
            case PlaceRemovedFromOrderEvent e -> apply(e);
            case OrderConfirmedEvent e -> apply(e);
            case OrderCancelledEvent e -> apply(e);
        }
        setRevision(orderDomainEvent.revision());
    }

    private void apply(final OrderStartedEvent ignored)
    {
        status = OrderStatus.STARTED;
    }

    private void apply(final OrderSubmittedEvent ignored) throws NoPlacesAddedException, OrderNotStartedException
    {
        if (status != OrderStatus.STARTED)
        {
            throw new OrderNotStartedException(id);
        }

        if (places.isEmpty())
        {
            throw new NoPlacesAddedException(id);
        }

        status = OrderStatus.SUBMITTED;
    }

    private void apply(final PlaceAddedToOrderEvent e) throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        if (status != OrderStatus.STARTED)
        {
            throw new OrderNotStartedException(id);
        }

        final PlaceId placeId = e.placeId();
        if (!places.add(placeId))
        {
            throw new PlaceAlreadyAddedException(placeId);
        }
    }

    private void apply(final PlaceRemovedFromOrderEvent e) throws PlaceNotAddedException, OrderNotStartedException
    {
        if (status != OrderStatus.STARTED)
        {
            throw new OrderNotStartedException(id);
        }

        final PlaceId placeId = e.placeId();
        if (!places.remove(placeId))
        {
            throw new PlaceNotAddedException(placeId);
        }
    }

    private void apply(final OrderConfirmedEvent ignored) throws OrderNotSubmittedException
    {
        if (status != OrderStatus.SUBMITTED)
        {
            throw new OrderNotSubmittedException(id);
        }

        status = OrderStatus.CONFIRMED;
    }

    private void apply(final OrderCancelledEvent ignored) throws OrderAlreadyCancelledException,
            ConfirmedOrderCanNotBeCancelledException
    {
        if (status == OrderStatus.CANCELLED)
        {
            throw new OrderAlreadyCancelledException(id);
        }

        if (status == OrderStatus.CONFIRMED)
        {
            throw new ConfirmedOrderCanNotBeCancelledException(id);
        }

        status = OrderStatus.CANCELLED;
        places.clear();
    }
}
