package kz.hackload.ticketing.service.provider.domain.places;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.InMemoryOrdersRepository;
import kz.hackload.ticketing.service.provider.domain.InMemoryPlacesRepository;
import kz.hackload.ticketing.service.provider.domain.orders.*;

public class SelectPlaceServiceTest
{
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    @Test
    void shouldSelectPlaceForStartedOrder() throws PlaceAlreadySelectedException, OrderNotStartedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException, PlaceCanNotBeAddedToOrderException
    {
        final OrderId orderId = ordersRepository.nextId();
        final Order order = Order.start(orderId);

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place = Place.create(placeId, row, seat);

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

        final var row = new Row(1);
        final var seat = new Seat(1);
        final var placeId = placesRepository.nextId();
        final var place1 = Place.create(placeId, row, seat);

        selectPlaceService.selectPlaceForOrder(place1, order);
        addPlaceToOrderService.addPlace(order, place1);
        order.submit();

        final var place2Id = placesRepository.nextId();
        final var place2 = Place.create(place2Id, row, seat);

        assertThatThrownBy(() -> selectPlaceService.selectPlaceForOrder(place2, order))
                .isInstanceOf(PlaceCanNotBeAddedToOrderException.class)
                .hasMessage("Place %s can not be added to order %s".formatted(place2Id, orderId));
    }
}
