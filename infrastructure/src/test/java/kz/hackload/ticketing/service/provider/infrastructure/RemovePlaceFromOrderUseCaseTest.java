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
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesResourceJavalinHttpAdapter;

public class RemovePlaceFromOrderUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, removePlaceFromOrderUseCase);
    }

    @Test
    void placeReleased() throws PlaceCanNotBeAddedToOrderException, PlaceAlreadySelectedException
    {
        // given
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

                Awaitility.await()
                        .atMost(Duration.ofSeconds(60L))
                        .until(() -> transactionManager.executeInTransaction(() -> placesRepository.findById(placeId)
                                .map(Place::isFree)
                                .orElse(false))
                        );

                final Optional<Place> optionalPlace = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId));

                final Place place = assertThat(optionalPlace)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(place.isFree()).isTrue();
                assertThat(place.selectedFor()).isEmpty();
                assertThat(place.isSelectedFor(orderId)).isFalse();
            }
        });
    }
}
