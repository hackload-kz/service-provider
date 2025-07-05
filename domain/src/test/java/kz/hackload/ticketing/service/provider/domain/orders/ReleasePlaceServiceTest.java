package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadyReleasedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceReleasedEvent;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public class ReleasePlaceServiceTest
{
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
    private final ReleasePlaceService releasePlaceService = new ReleasePlaceService();

    @Test
    void shouldReleasePlace() throws PlaceAlreadyReleasedException, PlaceAlreadySelectedException
    {
        final OrderId orderId = new OrderId(UUID.randomUUID());
        final Order order = Order.start(orderId);
        order.commitEvents();

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(UUID.randomUUID());
        final var place = Place.create(placeId, row, seat);
        place.selectFor(orderId);
        place.commitEvents();

        releasePlaceService.release(order, place);

        assertThat(place.isFree()).isTrue();

        assertThat(place.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new PlaceReleasedEvent(orderId));
    }
}
