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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;

public class SelectPlaceUseCaseTest extends AbstractIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(SelectPlaceUseCaseTest.class);

    @BeforeEach
    void setUp()
    {
        new PlaceResourceJavalinHttpAdapter(server, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
    }

    @Test
    void placeSelected()
    {
        // given
        clocks.setClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        JavalinTest.test(server, (_, c) ->
        {
            // when
            try (final Response response = c.patch("/api/partners/v1/places/" + placeId.value() + "/select", """
                    {
                        "order_id": "%s",
                        "place_id": "%s"
                    }
                    """.formatted(orderId.value(), placeId.value())))
            {

                // then
                assertThat(response.isSuccessful()).isTrue();
                final Optional<Place> optionalPlace = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId));

                final Place actual = assertThat(optionalPlace)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(actual.isFree()).isFalse();
                assertThat(actual.selectedFor()).isPresent().get().isEqualTo(orderId);
                assertThat(actual.isSelectedFor(orderId)).isTrue();
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                            .map(order -> order.contains(placeId))
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
                            1)
                    );
                }
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)
                            .map(order -> !order.isFree())
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
                                """.formatted(placeId, row, seat, false)
                    );
                }
            }
        });
    }
}
