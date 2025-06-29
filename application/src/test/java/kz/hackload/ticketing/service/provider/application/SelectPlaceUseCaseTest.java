package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class SelectPlaceUseCaseTest
{
    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();

    private final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(placesRepository);
    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);

    @Test
    void shouldSelectPlace() throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException, AggregateRestoreException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

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
