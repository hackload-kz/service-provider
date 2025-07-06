package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Optional;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesResourceJavalinHttpAdapter;

public class SelectPlaceUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, removePlaceFromOrderUseCase);
    }

    @Test
    void placeSelected()
    {
        // given
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

                Awaitility.await()
                        .atMost(Duration.ofSeconds(60L))
                        .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                                .map(order -> order.contains(placeId))
                                .orElse(false))
                        );

                final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
                assertThat(order.contains(placeId)).isTrue();
            }
        });
    }
}
