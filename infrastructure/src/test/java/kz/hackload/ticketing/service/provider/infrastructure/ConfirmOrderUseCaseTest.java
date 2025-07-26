package kz.hackload.ticketing.service.provider.infrastructure;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import kz.hackload.ticketing.service.provider.domain.orders.NoPlacesAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;

public class ConfirmOrderUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
    }

    @Test
    void orderConfirmed() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException
    {
        // given
        final Instant startTime = Instant.now();
        clocks.setClock(Clock.fixed(startTime, ZoneId.systemDefault()));

        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);

        final Instant submitTime = Instant.now();
        clocks.setClock(Clock.fixed(submitTime, ZoneId.systemDefault()));
        submitOrderUseCase.submit(orderId);

        JavalinTest.test(server, (_, c) ->
        {
            final Instant confirmTime = Instant.now();
            clocks.setClock(Clock.fixed(confirmTime, ZoneId.systemDefault()));
            // when
            try (final Response response = c.patch("/api/partners/v1/orders/" + orderId.value() + "/confirm"))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();

                final Order actual = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

                assertThat(actual.status()).isEqualTo(OrderStatus.CONFIRMED);
                assertThat(actual.places()).hasSize(1).first().isEqualTo(placeId);
                assertThat(actual.contains(placeId)).isTrue();
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> ordersQueryRepository.getOrder(orderId).map(o -> o.status() == OrderStatus.CONFIRMED).orElse(false));

            try (final Response startedOrderResponse = c.get("/api/partners/v1/orders/" + orderId))
            {
                assertThat(startedOrderResponse.isSuccessful()).isTrue();

                try (final ResponseBody order = startedOrderResponse.body())
                {
                    assertThat(order).isNotNull();
                    assertThatJson(order.string()).isEqualTo("""
                                {
                                    "id": "%s",
                                    "status": "%s",
                                    "started_at": %s,
                                    "updated_at": %s,
                                    "places_count": %s
                                }
                                """.formatted(
                            orderId,
                            "CONFIRMED",
                            startTime.toEpochMilli(),
                            confirmTime.toEpochMilli(),
                            1)
                    );
                }
            }
        });
    }
}
