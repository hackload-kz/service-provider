package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class RemovePlaceFromOrderServiceTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final AddPlaceToOrderService addService = new AddPlaceToOrderService();
    private final RemovePlaceFromOrderService removeService = new RemovePlaceFromOrderService();

    @Test
    void shouldRemovePlaceFromOrder() throws PlaceAlreadySelectedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceNotAddedException, OrderNotStartedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(row, seat);
        final Place place = Place.create(placeId);
        place.selectFor(orderId);
        addService.addPlace(order, place);
        order.commitEvents();

        removeService.removePlace(order, place);

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents())
                .hasSize(1)
                .first()
                .isEqualTo(new PlaceRemovedFromOrderEvent(placeId));
    }

    @Test
    void shouldNotRemovePlaceSelectedForAnotherOrder() throws PlaceAlreadySelectedException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = new PlaceId(row, seat);

        final OrderId anotherOrderId = ordersRepository.nextId();
        final Place place = Place.create(placeId);
        place.selectFor(anotherOrderId);

        assertThatThrownBy(() -> removeService.removePlace(order, place))
                .isInstanceOf(PlaceSelectedForAnotherOrderException.class)
                .hasMessage("Place %s is selected for order %s not for order %s".formatted(placeId, anotherOrderId, orderId));

        assertThat(order.places()).isEmpty();

        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
