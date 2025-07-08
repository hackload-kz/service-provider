package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

public class OrdersTest
{
    @Test
    void shouldCreateOrder()
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderStartedEvent(now, 1));

        order.commitEvents();

        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldAddPlace() throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);

        assertThat(order.places())
                .hasSize(1)
                .first()
                .isEqualTo(placeId);

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceAddedToOrderEvent(now, 2, placeId));
    }

    @Test
    void shouldHandlePlaceReleasedEvent() throws PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.commitEvents();

        order.removePlace(now, placeId);

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceRemovedFromOrderEvent(now, 3, placeId));
    }

    @Test
    void shouldNotHandleDuplicateSelectedPlaceEvent() throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.commitEvents();

        assertThatThrownBy(() -> order.addPlace(now, placeId))
                .isInstanceOf(PlaceAlreadyAddedException.class)
                .hasMessage("The place %s is already added".formatted(placeId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotHandleDuplicatePlaceReleasedEvent() throws PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);

        order.removePlace(now, placeId);
        order.commitEvents();

        assertThatThrownBy(() -> order.removePlace(now, placeId))
                .isInstanceOf(PlaceNotAddedException.class)
                .hasMessage("The place %s is not added".formatted(placeId));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldSubmitOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.commitEvents();

        order.submit(now);

        assertThat(order.status()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderSubmittedEvent(now, 3));
    }

    @Test
    void shouldNotAddPlaceToSubmittedOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.submit(now);

        order.commitEvents();

        assertThatThrownBy(() -> order.addPlace(now, placeId))
                .isInstanceOf(OrderNotStartedException.class)
                .hasMessage("Order %s is not started".formatted(orderId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotRemovePlaceFromNotStartedOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.submit(now);

        order.commitEvents();

        assertThatThrownBy(() -> order.removePlace(now, placeId))
                .isInstanceOf(OrderNotStartedException.class)
                .hasMessage("Order %s is not started".formatted(orderId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotSubmitOrderWithoutAddedPlaces()
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        assertThatThrownBy(() -> order.submit(now))
                .isInstanceOf(NoPlacesAddedException.class)
                .hasMessage("Order %s does not have any place in it");

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldRestoreOrderFromEvents()
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        List<OrderDomainEvent> envelopes = List.of(new OrderStartedEvent(now, 0));

        final Order order = Order.restore(orderId, envelopes);

        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
        assertThat(order.revision()).isEqualTo(0L);
    }

    @Test
    void shouldNotRestoreOrderFromEvents()
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        List<OrderDomainEvent> events = List.of(new OrderStartedEvent(now, 0L), new PlaceAddedToOrderEvent(now, 1L, placeId), new PlaceAddedToOrderEvent(now, 2L, placeId));

        assertThatThrownBy(() -> Order.restore(orderId, events))
                .isInstanceOf(AggregateRestoreException.class)
                .hasMessage("kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException: The place %s is already added".formatted(placeId));
    }

    @Test
    void shouldConfirmOrder() throws OrderNotStartedException, PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotSubmittedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);
        order.submit(now);

        order.commitEvents();

        order.confirm(now);

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderConfirmedEvent(now, 4));
    }

    @Test
    void shouldNotConfirmNonSubmittedOrder() throws OrderNotStartedException, PlaceAlreadyAddedException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        order.addPlace(now, placeId);

        order.commitEvents();

        assertThatThrownBy(() -> order.confirm(now))
                .isInstanceOf(OrderNotSubmittedException.class)
                .hasMessage("Order %s is not submitted".formatted(orderId));

        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldCancelEmptyStartedOrder() throws OrderAlreadyCancelledException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        order.cancel(now);

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderCancelledEvent(now, 2, Set.of()));
    }

    @Test
    void shouldRemovePlacesFromOrderAndCancelStartedOrder() throws OrderNotStartedException, PlaceAlreadyAddedException, OrderAlreadyCancelledException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final PlaceId placeId = new PlaceId(UUID.randomUUID());
        order.addPlace(now, placeId);
        order.commitEvents();

        order.cancel(now);

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new OrderCancelledEvent(now, 3, Set.of(placeId)));
    }

    @Test
    void shouldRemovePlacesFromOrderAndCancelSubmittedOrder() throws OrderNotStartedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException, OrderAlreadyCancelledException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final PlaceId placeId = new PlaceId(UUID.randomUUID());
        order.addPlace(now, placeId);
        order.submit(now);
        order.commitEvents();

        order.cancel(now);

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new OrderCancelledEvent(now, 4, Set.of(placeId)));
    }

    @Test
    void shouldNotCancelCancelledOrder() throws OrderAlreadyCancelledException
    {
        final Instant now = Instant.now();

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        order.cancel(now);
        order.commitEvents();

        assertThatThrownBy(() -> order.cancel(now))
                .isInstanceOf(OrderAlreadyCancelledException.class)
                .hasMessage("Order %s is already cancelled".formatted(orderId));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
