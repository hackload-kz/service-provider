package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class CancelOrderUseCaseTest
{
    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    private final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(placesRepository);
    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);
    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final SubmitOrderUseCase submitOrderUseCase = new SubmitOrderApplicationService(ordersRepository);
    private final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(ordersRepository, placesRepository, addPlaceToOrderService);
    private final CancelOrderUseCase cancelOrderUseCase = new CancelOrderApplicationService(ordersRepository);

    @Test
    void orderCancelled() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            AggregateRestoreException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException,
            OrderAlreadyCancelledException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);
        submitOrderUseCase.submit(orderId);

        // when
        cancelOrderUseCase.cancel(orderId);

        // then
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        assertThat(order.places()).isEmpty();
        assertThat(order.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
