package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class ReleasePlaceServiceTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
    private final ReleasePlaceService releasePlaceService = new ReleasePlaceService();

    @Test
    void shouldReleasePlace() throws PlaceAlreadySelectedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceNotAddedException, PlaceAlreadyReleasedException, OrderStillContainsPlaceException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = new PlaceId(row, seat);
        final var place = Place.create(placeId);

        place.selectFor(orderId);
        addPlaceToOrderService.addPlace(order, place);

        order.commitEvents();
        place.commitEvents();

        releasePlaceService.release(order, place);

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .containsExactly(new PlaceRemovedFromOrderEvent(placeId));
    }
}
