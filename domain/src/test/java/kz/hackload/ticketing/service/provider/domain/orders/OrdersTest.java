package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.*;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.InMemoryPlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public class OrdersTest
{
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();

    @Test
    void shouldCreateOrder()
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderStartedEvent());

        order.commitEvents();

        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldAddPlace() throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);

        assertThat(order.places())
                .hasSize(1)
                .first()
                .isEqualTo(placeId);

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceAddedToOrderEvent(placeId));
    }

    @Test
    void shouldHandlePlaceReleasedEvent() throws PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.commitEvents();

        order.removePlace(placeId);

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceRemovedFromOrderEvent(placeId));
    }

    @Test
    void shouldNotHandleDuplicateSelectedPlaceEvent() throws PlaceAlreadyAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.commitEvents();

        assertThatThrownBy(() -> order.addPlace(placeId))
                .isInstanceOf(PlaceAlreadyAddedException.class)
                .hasMessage("The place %s is already added".formatted(placeId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotHandleDuplicatePlaceReleasedEvent() throws PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);

        order.removePlace(placeId);
        order.commitEvents();

        assertThatThrownBy(() -> order.removePlace(placeId))
                .isInstanceOf(PlaceNotAddedException.class)
                .hasMessage("The place %s is not added".formatted(placeId));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldSubmitOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.commitEvents();

        order.submit();

        assertThat(order.status()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderSubmittedEvent());
    }

    @Test
    void shouldNotAddPlaceToSubmittedOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.submit();

        order.commitEvents();

        assertThatThrownBy(() -> order.addPlace(placeId))
                .isInstanceOf(OrderNotStartedException.class)
                .hasMessage("Order %s is not started".formatted(orderId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotRemovePlaceFromNotStartedOrder() throws PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.submit();

        order.commitEvents();

        assertThatThrownBy(() -> order.removePlace(placeId))
                .isInstanceOf(OrderNotStartedException.class)
                .hasMessage("Order %s is not started".formatted(orderId));

        assertThat(order.places()).hasSize(1);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotSubmitOrderWithoutAddedPlaces()
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        assertThatThrownBy(order::submit)
                .isInstanceOf(NoPlacesAddedException.class)
                .hasMessage("Order %s does not have any place in it");

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldRestoreOrderFromEvents() throws AggregateRestoreException
    {
        final OrderId orderId = ordersRepository.nextId();
        List<OrderDomainEvent> envelopes = List.of(new OrderStartedEvent());

        final Order order = Order.restore(orderId, 1L, envelopes);

        assertThat(order.id()).isEqualTo(orderId);
        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotRestoreOrderFromEvents()
    {
        final OrderId orderId = ordersRepository.nextId();
        final PlaceId placeId = placesRepository.nextId();

        List<OrderDomainEvent> events = List.of(new OrderStartedEvent(), new PlaceAddedToOrderEvent(placeId), new PlaceAddedToOrderEvent(placeId));

        assertThatThrownBy(() -> Order.restore(orderId, 2L, events))
                .isInstanceOf(AggregateRestoreException.class)
                .hasMessage("kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException: The place %s is already added".formatted(placeId));
    }

    @Test
    void shouldConfirmOrder() throws OrderNotStartedException, PlaceAlreadyAddedException, NoPlacesAddedException, OrderNotSubmittedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);
        order.submit();

        order.commitEvents();

        order.confirm();

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderConfirmedEvent());
    }

    @Test
    void shouldNotConfirmNonSubmittedOrder() throws OrderNotStartedException, PlaceAlreadyAddedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final PlaceId placeId = placesRepository.nextId();

        order.addPlace(placeId);

        order.commitEvents();

        assertThatThrownBy(order::confirm)
                .isInstanceOf(OrderNotSubmittedException.class)
                .hasMessage("Order %s is not submitted".formatted(orderId));

        assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldCancelEmptyStartedOrder() throws OrderAlreadyCancelledException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        order.cancel();

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new OrderCancelledEvent(Set.of()));
    }

    @Test
    void shouldRemovePlacesFromOrderAndCancelStartedOrder() throws OrderNotStartedException, PlaceAlreadyAddedException, OrderAlreadyCancelledException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();
        order.addPlace(placeId);
        order.commitEvents();

        order.cancel();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new OrderCancelledEvent(Set.of(placeId)));
    }

    @Test
    void shouldRemovePlacesFromOrderAndCancelSubmittedOrder() throws OrderNotStartedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException, OrderAlreadyCancelledException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = placesRepository.nextId();
        order.addPlace(placeId);
        order.submit();
        order.commitEvents();

        order.cancel();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new OrderCancelledEvent(Set.of(placeId)));
    }

    @Test
    void shouldNotCancelCancelledOrder() throws OrderAlreadyCancelledException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        order.cancel();
        order.commitEvents();

        assertThatThrownBy(order::cancel)
                .isInstanceOf(OrderAlreadyCancelledException.class)
                .hasMessage("Order %s is already cancelled".formatted(orderId));

        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
