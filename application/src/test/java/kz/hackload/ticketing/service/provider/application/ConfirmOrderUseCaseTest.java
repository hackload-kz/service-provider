package kz.hackload.ticketing.service.provider.application;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class ConfirmOrderUseCaseTest
{
    private final OrdersRepository ordersRepository = new InMemoryOrdersRepository();
    private final PlacesRepository placesRepository = new InMemoryPlacesRepository();

    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    private final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(placesRepository);
    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);
    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final SubmitOrderUseCase submitOrderUseCase = new SubmitOrderApplicationService(ordersRepository);
    private final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(ordersRepository, placesRepository, addPlaceToOrderService);
    private final ConfirmOrderUseCase confirmOrderUseCase = new ConfirmOrderApplicationService(ordersRepository);

    @Test
    void shouldConfirmOrder() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException, OrderNotSubmittedException
    {
        // given
        final PlaceId placeId = new PlaceId(new Row(1), new Seat(1));
        createPlaceUseCase.create(placeId);

        final OrderId orderId = startOrderUseCase.startOrder();
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);
        submitOrderUseCase.submit(orderId);

        // when
        confirmOrderUseCase.confirm(orderId);

        // then
        final Order order = ordersRepository.findById(orderId).orElseThrow();
        assertThat(order.places())
                .hasSize(1)
                .first()
                .isEqualTo(placeId);

        assertThat(order.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.uncommittedEvents()).isEmpty();
    }
}
