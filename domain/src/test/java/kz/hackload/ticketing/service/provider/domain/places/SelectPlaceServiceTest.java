package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.FakeClock;
import kz.hackload.ticketing.service.provider.domain.orders.AddPlaceToOrderService;
import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;

public class SelectPlaceServiceTest
{
    private final FakeClock clocks = new FakeClock();
    private final SelectPlaceService selectPlaceService = new SelectPlaceService(clocks);
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService(clocks);

    @Test
    void shouldSelectPlaceForStartedOrder() throws PlaceAlreadySelectedException, OrderNotStartedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceCanNotBeAddedToOrderException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(now, placeId, row, seat);

        selectPlaceService.selectPlaceForOrder(place, order);

        assertThat(place.isFree()).isFalse();
        assertThat(place.isSelectedFor(orderId)).isTrue();
    }

    @Test
    void shouldNotSelectPlaceForNotStartedOrder() throws PlaceAlreadySelectedException,
            OrderNotStartedException,
            NoPlacesAddedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceCanNotBeAddedToOrderException
    {
        final Instant now = Instant.now();
        clocks.setClock(Clock.fixed(now, ZoneId.systemDefault()));

        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(now, orderId);

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place1 = Place.create(now, placeId, row, seat);

        selectPlaceService.selectPlaceForOrder(place1, order);
        addPlaceToOrderService.addPlace(order, place1);
        order.submit(now);

        final var place2Id = new PlaceId(UUID.randomUUID());
        final var place2 = Place.create(now, place2Id, row, seat);

        assertThatThrownBy(() -> selectPlaceService.selectPlaceForOrder(place2, order))
                .isInstanceOf(PlaceCanNotBeAddedToOrderException.class)
                .hasMessage("Place %s can not be added to order %s".formatted(place2Id, orderId));
    }
}
