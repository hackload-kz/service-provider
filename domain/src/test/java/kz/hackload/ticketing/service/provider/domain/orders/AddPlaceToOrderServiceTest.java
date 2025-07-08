package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.FakeClock;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public class AddPlaceToOrderServiceTest
{
    private final FakeClock clocks = new FakeClock();
    private final AddPlaceToOrderService service = new AddPlaceToOrderService(clocks);

    @Test
    void shouldAddPlaceToOrder() throws PlaceAlreadyAddedException, PlaceSelectedForAnotherOrderException, PlaceAlreadySelectedException, PlaceIsNotSelectedException, OrderNotStartedException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        final Place place = Place.create(now, placeId, row, seat);
        place.selectFor(now, orderId);

        service.addPlace(order, place);

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
    void shouldNotAddPlaceWithAnotherOrderId() throws PlaceAlreadySelectedException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId augendOrder = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, augendOrder);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());
        final Place place = Place.create(now, placeId, row, seat);

        final OrderId selectedForOrder = new OrderId(UUID.randomUUID());
        place.selectFor(now, selectedForOrder);

        assertThatThrownBy(() -> service.addPlace(order, place))
                .isInstanceOf(PlaceSelectedForAnotherOrderException.class)
                .hasMessage("Place %s is selected for order %s not for order %s".formatted(placeId, selectedForOrder, augendOrder));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }

    @Test
    void shouldNotAddNonSelectedPlace()
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());

        final Place place = Place.create(now, placeId, row, seat);

        assertThatThrownBy(() -> service.addPlace(order, place))
                .isInstanceOf(PlaceIsNotSelectedException.class)
                .hasMessage("Place %s is not selected and can not be added to order %s".formatted(placeId, orderId));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
