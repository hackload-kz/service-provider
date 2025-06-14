package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.*;

public class SelectPlaceServiceTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    @Test
    void shouldSelectPlaceForStartedOrder() throws PlaceAlreadySelectedException, OrderNotStartedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceCanNotBeAddedToOrderException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        final Place place = Place.create(placeId);

        selectPlaceService.selectPlaceForOrder(place, order);

        assertThat(place.isFree()).isFalse();
        assertThat(place.isSelectedFor(orderId)).isTrue();
    }

    @Test
    void shouldNotSelectPlaceForNotStartedOrder() throws PlaceAlreadySelectedException,
            OrderNotStartedException,
            NoPlacesAddedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceCanNotBeAddedToOrderException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final PlaceId place1Id = new PlaceId(new Row(1), new Seat(1));
        final Place place1 = Place.create(place1Id);

        selectPlaceService.selectPlaceForOrder(place1, order);
        addPlaceToOrderService.addPlace(order, place1);
        order.submit();

        final PlaceId place2Id = new PlaceId(new Row(1), new Seat(2));
        final Place place2 = Place.create(place2Id);

        assertThatThrownBy(() -> selectPlaceService.selectPlaceForOrder(place2, order))
                .isInstanceOf(PlaceCanNotBeAddedToOrderException.class)
                .hasMessage("Place %s can not be added to order %s".formatted(place2Id, orderId));
    }
}
