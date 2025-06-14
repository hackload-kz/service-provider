package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class ReleasePlaceUseCaseTest
{
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final ReleasePlaceService releasePlaceService = new ReleasePlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);
    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(releasePlaceService, placesRepository, ordersRepository);

    @Test
    void shouldReleasePlace() throws PlaceCanNotBeAddedToOrderException, PlaceAlreadySelectedException, PlaceAlreadyReleasedException, OrderNotStartedException, PlaceNotAddedException, PlaceIsNotSelectedException, PlaceSelectedForAnotherOrderException, PlaceAlreadyAddedException
    {
        // given
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        placesRepository.save(Place.create(placeId));

        final OrderId orderId = startOrderUseCase.startOrder();

        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        final Place place1 = placesRepository.findById(placeId).orElseThrow();
        addPlaceToOrderService.addPlace(order, place1);
        ordersRepository.save(order);
        placesRepository.save(place1);

        // when
        releasePlaceUseCase.releasePlace(placeId);

        // then
        final Order result = ordersRepository.findById(orderId).orElseThrow();
        assertThat(result.places()).isEmpty();
        assertThat(result.uncommittedEvents()).isEmpty();
    }
}
