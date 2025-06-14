package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class SelectPlaceUseCaseTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();

    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);

    @Test
    void shouldSelectPlace() throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException
    {
        // given
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        placesRepository.save(Place.create(placeId));

        final OrderId orderId = startOrderUseCase.startOrder();

        // when
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);

        // then
        final Place place = placesRepository.findById(placeId).orElseThrow();
        assertThat(place.isFree()).isFalse();
        assertThat(place.isSelectedFor(orderId)).isTrue();
        assertThat(place.selectedFor()).isPresent().get().isEqualTo(orderId);
    }
}
