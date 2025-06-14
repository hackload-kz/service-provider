package kz.hackload.ticketing.service.provider.domain.orders;

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
        this(orderId, OrderStatus.STARTED);
    }


    private Order(final OrderId orderId, final OrderStatus orderStatus)
    {
        super(orderId);

        this.places = new HashSet<>();
        this.status = orderStatus;
    }

    public static Order start(final OrderId orderId)
    {
        final Order order = new Order(orderId);
        final OrderStartedEvent event = new OrderStartedEvent();

        order.addEvent(event);

        return order;
    }

    public static Order restore(final OrderId id, final long revision, final List<OrderDomainEvent> events)
            throws AggregateRestoreException
    {
        final Order order = new Order(id);
        for (OrderDomainEvent event : events)
        {
            try
            {
                order.apply(event);
            }
            catch (final PlaceNotAddedException | PlaceAlreadyAddedException | NoPlacesAddedException |
                         OrderNotStartedException | OrderNotSubmittedException | OrderAlreadyCancelledException e)
            {
                throw new AggregateRestoreException(e);
            }
        }

        order.setRevision(revision);

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

    public void addPlace(final PlaceId placeId) throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final PlaceAddedToOrderEvent event = new PlaceAddedToOrderEvent(placeId);

        apply(event);
        addEvent(event);
    }

    public void removePlace(final PlaceId placeId) throws PlaceNotAddedException, OrderNotStartedException
    {
        final PlaceRemovedFromOrderEvent event = new PlaceRemovedFromOrderEvent(placeId);
        apply(event);
        addEvent(event);
    }

    public void submit() throws NoPlacesAddedException, OrderNotStartedException
    {
        final OrderSubmittedEvent event = new OrderSubmittedEvent();
        apply(event);
        addEvent(event);
    }

    public void confirm() throws OrderNotSubmittedException
    {
        final OrderConfirmedEvent orderConfirmedEvent = new OrderConfirmedEvent();
        apply(orderConfirmedEvent);
        addEvent(orderConfirmedEvent);
    }

    public void cancel() throws OrderAlreadyCancelledException
    {
        final OrderCancelledEvent orderCancelledEvent = new OrderCancelledEvent();
        apply(orderCancelledEvent);
        addEvent(orderCancelledEvent);
    }

    private void apply(final OrderDomainEvent orderDomainEvent) throws PlaceNotAddedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException,
            OrderNotStartedException,
            OrderNotSubmittedException,
            OrderAlreadyCancelledException
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

    private void apply(final OrderCancelledEvent ignored) throws OrderAlreadyCancelledException
    {
        if (status == OrderStatus.CANCELLED)
        {
            throw new OrderAlreadyCancelledException(id);
        }

        status = OrderStatus.CANCELLED;
        for (final PlaceId place : places)
        {
            addEvent(new PlaceRemovedFromOrderEvent(place));
        }
        places.clear();
    }
}
