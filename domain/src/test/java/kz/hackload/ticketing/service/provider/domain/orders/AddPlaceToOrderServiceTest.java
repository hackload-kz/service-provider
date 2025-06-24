package kz.hackload.ticketing.service.provider.domain.orders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.OrdersRepositoryInMemoryAdapter;
import kz.hackload.ticketing.service.provider.domain.PlacesRepositoryInMemoryAdapter;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class AddPlaceToOrderServiceTest
{
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();
    private final OrdersRepository repository = new OrdersRepositoryInMemoryAdapter();
    private final AddPlaceToOrderService service = new AddPlaceToOrderService();

    @Test
    void shouldAddPlaceToOrder() throws PlaceAlreadyAddedException, PlaceSelectedForAnotherOrderException, PlaceAlreadySelectedException, PlaceIsNotSelectedException, OrderNotStartedException
    {
        final OrderId orderId = repository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = placesRepository.nextId();

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
        final OrderId augendOrder = repository.nextId();
        final Order order = Order.start(augendOrder);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = placesRepository.nextId();
        final Place place = Place.create(placeId, row, seat);

        final OrderId selectedForOrder = repository.nextId();
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
        final OrderId orderId = repository.nextId();
        final Order order = Order.start(orderId);
        order.commitEvents();

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = placesRepository.nextId();

        final Place place = Place.create(placeId, row, seat);

        assertThatThrownBy(() -> service.addPlace(order, place))
                .isInstanceOf(PlaceIsNotSelectedException.class)
                .hasMessage("Place %s is not selected and can not be added to order %s".formatted(placeId, orderId));

        assertThat(order.places()).isEmpty();
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
