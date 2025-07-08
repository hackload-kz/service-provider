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

public class RemovePlaceFromOrderServiceTest
{
    private final FakeClock clocks = new FakeClock();
    private final AddPlaceToOrderService addService = new AddPlaceToOrderService(clocks);
    private final RemovePlaceFromOrderService removeService = new RemovePlaceFromOrderService(clocks);

    @Test
    void shouldRemovePlaceFromOrder() throws PlaceAlreadySelectedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final var row = new Row(1);
        final var seat = new Seat(1);
        final PlaceId placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);
        place.selectFor(now, orderId);
        addService.addPlace(order, place);
        order.commitEvents();

        removeService.removePlace(order, place);

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceRemovedFromOrderEvent(now, 3, placeId));
    }

    @Test
    void shouldNotRemovePlaceSelectedForAnotherOrder() throws PlaceAlreadySelectedException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);
        order.commitEvents();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        final OrderId anotherOrderId = new OrderId(UUID.randomUUID());
        place.selectFor(now, anotherOrderId);

        assertThatThrownBy(() -> removeService.removePlace(order, place))
                .isInstanceOf(PlaceSelectedForAnotherOrderException.class)
                .hasMessage("Place %s is selected for order %s not for order %s".formatted(placeId, anotherOrderId, orderId));

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
