package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import kz.hackload.ticketing.service.provider.application.*;
import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.OrdersRepositoryInMemoryAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.PlacesRepositoryInMemoryAdapter;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OrderResourcesTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();

    private final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(placesRepository);
    private final SelectPlaceService selectPlaceService = new SelectPlaceService();
    private final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();

    private final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(ordersRepository);
    private final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, placesRepository, ordersRepository);
    private final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(ordersRepository, placesRepository, addPlaceToOrderService);
    private final SubmitOrderUseCase submitOrderUseCase = new SubmitOrderApplicationService(ordersRepository);
    private final ConfirmOrderUseCase confirmOrderUseCase = new ConfirmOrderApplicationService(ordersRepository);

    private OrderResourcesJavalinHttpAdapter adapter;
    private Javalin server;

    @BeforeEach
    void setUp()
    {
        server = Javalin.create();
        adapter = new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase);
    }

    @AfterEach
    void tearDown()
    {
        server.stop();
    }

    @Test
    void orderStarted()
    {
        JavalinTest.test(server, (s, c) ->
        {
            try (final Response response = c.post("/api/partners/v1/orders"))
            {
                assertThat(response.isSuccessful()).isTrue();
                try (final ResponseBody body = response.body())
                {
                    assertThat(body).isNotNull();
                    final StartedOrderDto startedOrderDto = MAPPER.readValue(body.string(), StartedOrderDto.class);
                    final OrderId orderId = new OrderId(UUID.fromString(startedOrderDto.orderId()));

                    final Order order = assertThat(ordersRepository.findById(orderId)).isPresent().actual().get();
                    assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
                    assertThat(order.places()).isEmpty();
                }
            }
        });
    }

    @Test
    void orderSubmitted() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            AggregateRestoreException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException
    {
        final PlaceId placeId = createPlaceUseCase.create(new Row(1), new Seat(2));
        final OrderId orderId = startOrderUseCase.startOrder();

        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);

        JavalinTest.test(server, (s, c) ->
        {
            try (final Response response = c.patch("/api/partners/v1/orders/" + orderId.value() + "/submit"))
            {
                assertThat(response.isSuccessful()).isTrue();

                final Order actual = ordersRepository.findById(orderId).orElseThrow();

                assertThat(actual.status()).isEqualTo(OrderStatus.SUBMITTED);
                assertThat(actual.places()).hasSize(1).first().isEqualTo(placeId);
                assertThat(actual.contains(placeId)).isTrue();
            }
        });
    }

    @Test
    void orderConfirmed() throws OrderNotStartedException,
            NoPlacesAddedException,
            AggregateRestoreException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException
    {
        final PlaceId placeId = createPlaceUseCase.create(new Row(1), new Seat(2));
        final OrderId orderId = startOrderUseCase.startOrder();
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);
        submitOrderUseCase.submit(orderId);

        JavalinTest.test(server, (s, c) ->
        {
            try (final Response response = c.patch("/api/partners/v1/orders/" + orderId.value() + "/confirm"))
            {
                assertThat(response.isSuccessful()).isTrue();

                final Order actual = ordersRepository.findById(orderId).orElseThrow();

                assertThat(actual.status()).isEqualTo(OrderStatus.CONFIRMED);
                assertThat(actual.places()).hasSize(1).first().isEqualTo(placeId);
                assertThat(actual.contains(placeId)).isTrue();
            }
        });
    }
}
