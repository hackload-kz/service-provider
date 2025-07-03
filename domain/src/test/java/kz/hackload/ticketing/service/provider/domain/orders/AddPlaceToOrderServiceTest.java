package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public class AddPlaceToOrderServiceTest
{
    private final AddPlaceToOrderService service = new AddPlaceToOrderService();

    @Test
    void shouldAddPlaceToOrder() throws PlaceAlreadyAddedException, PlaceSelectedForAnotherOrderException, PlaceAlreadySelectedException, PlaceIsNotSelectedException, OrderNotStartedException
    {
        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        final Place place = Place.create(placeId, row, seat);
        place.selectFor(orderId);

        service.addPlace(order, place);

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
    void shouldNotAddPlaceWithAnotherOrderId() throws PlaceAlreadySelectedException
    {
        final OrderId augendOrder = new OrderId(UUID.randomUUID());
        final Order order = Order.start(augendOrder);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());
        final Place place = Place.create(placeId, row, seat);

        final OrderId selectedForOrder = new OrderId(UUID.randomUUID());
        place.selectFor(selectedForOrder);

        assertThatThrownBy(() -> service.addPlace(order, place))
                .isInstanceOf(PlaceSelectedForAnotherOrderException.class)
                .hasMessage("Place %s is selected for order %s not for order %s".formatted(placeId, selectedForOrder, augendOrder));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotAddNonSelectedPlace()
    {
        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        final Place place = Place.create(placeId, row, seat);

        assertThatThrownBy(() -> service.addPlace(order, place))
                .isInstanceOf(PlaceIsNotSelectedException.class)
                .hasMessage("Place %s is not selected and can not be added to order %s".formatted(placeId, orderId));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
