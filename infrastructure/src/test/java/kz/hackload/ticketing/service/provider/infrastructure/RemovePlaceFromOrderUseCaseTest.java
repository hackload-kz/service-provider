package kz.hackload.ticketing.service.provider.infrastructure;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;

public class RemovePlaceFromOrderUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new PlaceResourceJavalinHttpAdapter(server, createPlaceUseCase, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
    }

    @Test
    void placeReleased() throws PlaceCanNotBeAddedToOrderException, PlaceAlreadySelectedException
    {
        // given
        clocks.setClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        selectPlaceUseCase.selectPlaceFor(placeId, orderId);

        Awaitility.await()
                .atMost(Duration.ofSeconds(60L))
                .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                        .map(order -> order.contains(placeId))
                        .orElse(false))
                );

        JavalinTest.test(server, (_, c) ->
        {
            // when
            try (final Response response = c.patch("/api/partners/v1/places/" + placeId.value() + "/release", """
                    {
                        "order_id": "%s",
                        "place_id": "%s"
                    }
                    """.formatted(orderId.value(), placeId.value())))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();
                final Optional<Order> optionalOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId));

                final Order actual = assertThat(optionalOrder)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(actual.contains(placeId)).isFalse();
                assertThat(actual.places()).isEmpty();
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesRepository.findById(placeId)
                            .map(Place::isFree)
                            .orElse(false))
                    );

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
                                    "started_at": "%s",
                                    "updated_at": "%s",
                                    "places_count": %s
                                }
                                """.formatted(
                            orderId,
                            "STARTED",
                            DateTimeFormatter.ISO_INSTANT.format(clocks.now().truncatedTo(ChronoUnit.SECONDS)),
                            DateTimeFormatter.ISO_INSTANT.format(clocks.now().truncatedTo(ChronoUnit.SECONDS)),
                            0)
                    );
                }
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)
                            .map(GetPlaceQueryResult::isFree)
                            .orElse(false))
                    );

            try (final Response startedOrderResponse = c.get("/api/partners/v1/places/" + placeId))
            {
                assertThat(startedOrderResponse.isSuccessful()).isTrue();

                try (final ResponseBody place = startedOrderResponse.body())
                {
                    assertThat(place).isNotNull();
                    assertThatJson(place.string()).isEqualTo("""
                                {
                                    "id": "%s",
                                    "row": %s,
                                    "seat": %s,
                                    "is_free": %s
                                }
                                """.formatted(placeId, row, seat, true)
                    );
                }
            }
        });
    }
}
