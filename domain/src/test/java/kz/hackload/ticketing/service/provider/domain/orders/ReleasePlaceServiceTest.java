package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.FakeClock;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceReleasedEvent;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public class ReleasePlaceServiceTest
{
    private final FakeClock clocks = new FakeClock();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService(clocks);
    private final ReleasePlaceService releasePlaceService = new ReleasePlaceService(clocks);

    @Test
    void shouldReleasePlace() throws PlaceAlreadyReleasedException, PlaceAlreadySelectedException
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
        place.selectFor(now, orderId);
        place.commitEvents();

        releasePlaceService.release(order, place);

        assertThat(place.isFree()).isTrue();

        assertThat(place.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new PlaceReleasedEvent(now, 3, orderId));
    }
}
