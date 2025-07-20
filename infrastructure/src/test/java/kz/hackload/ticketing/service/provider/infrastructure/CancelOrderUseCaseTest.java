package kz.hackload.ticketing.service.provider.infrastructure;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderDto;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesTestDto;

public class CancelOrderUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
        new PlaceResourceJavalinHttpAdapter(server, createPlaceUseCase, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);
    }

    @Test
    void submittedOrderCancelled() throws PlaceCanNotBeAddedToOrderException,
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
        submitOrderUseCase.submit(orderId);

        JavalinTest.test(server, (_, c) ->
        {
            final Instant cancelTime = Instant.now();
            clocks.setClock(Clock.fixed(cancelTime, ZoneId.systemDefault()));

            // when
            try (final Response response = c.patch("/api/partners/v1/orders/" + orderId.value() + "/cancel"))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();

                final Order actual = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

                assertThat(actual.status()).isEqualTo(OrderStatus.CANCELLED);
                assertThat(actual.places()).isEmpty();
                assertThat(actual.contains(placeId)).isFalse();
            }

            Awaitility.await()
                    .atMost(Duration.ofSeconds(10L))
                    .until(() -> ordersQueryRepository.getOrder(orderId).map(o -> o.status() == OrderStatus.CANCELLED).orElse(false));

            try (final Response getOrderResponse = c.get("/api/partners/v1/orders/" + orderId))
            {
                assertThat(getOrderResponse.isSuccessful()).isTrue();

                try (final ResponseBody order = getOrderResponse.body())
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
                            "CANCELLED",
                            DateTimeFormatter.ISO_INSTANT.format(startTime.truncatedTo(ChronoUnit.SECONDS)),
                            DateTimeFormatter.ISO_INSTANT.format(cancelTime.truncatedTo(ChronoUnit.SECONDS)),
                            0)
                    );
                }
            }
        });
    }

    @Test
    void startedOrderCancelled()
    {
        // given
        JavalinTest.test(server, (s, c) ->
        {
            clocks.setClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
            final Row row = new Row(1);
            final Seat seat = new Seat(1);

            // create a place
            final PlaceId placeId;
            try (final Response createPlaceResponse = c.post("/api/admin/v1/places", "{\"row\": %s,\"seat\": %s}".formatted(row, seat)))
            {
                assertThat(createPlaceResponse.isSuccessful()).isTrue();
                final ResponseBody responseBody = Objects.requireNonNull(createPlaceResponse.body());
                final Map<?, ?> map = jsonMapper.fromJson(responseBody.string(), Map.class);
                placeId = new PlaceId(UUID.fromString((String) map.get("place_id")));
            }

            // start a new order
            final OrderId orderId;
            try (final Response startOrderResponse = c.post("/api/partners/v1/orders"))
            {
                assertThat(startOrderResponse.isSuccessful()).isTrue();
                final ResponseBody responseBody = Objects.requireNonNull(startOrderResponse.body());
                final Map<?, ?> map = jsonMapper.fromJson(responseBody.string(), Map.class);
                orderId = new OrderId(UUID.fromString((String) map.get("order_id")));
            }

            // select the created place for the started order
            try (final Response _ = c.patch("/api/partners/v1/places/" + placeId.value() + "/select", """
                    {
                        "order_id": "%s"
                    }
                    """.formatted(orderId.value())))
            {
                // nothing
            }

            // wait until the order contains the place
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15L))
                    .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                            .map(order -> order.contains(placeId))
                            .orElse(false))
                    );

            // wait until the place's projection
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)
                            .map(order -> !order.isFree())
                            .orElse(false))
                    );

            // when
            try (final Response cancelOrderResponse = c.patch("/api/partners/v1/orders/" + orderId + "/cancel"))
            {
                assertThat(cancelOrderResponse.isSuccessful()).isTrue();
            }

            // verify the place was free up
            Awaitility.await()
                    .atMost(Duration.ofSeconds(15L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)
                            .map(GetPlaceQueryResult::isFree)
                            .orElse(false))
                    );

            Awaitility.await()
                    .atMost(Duration.ofSeconds(15L))
                    .until(() -> transactionManager.executeInTransaction(() -> placesQueryRepository.getPlace(placeId)
                            .map(GetPlaceQueryResult::isFree)
                            .orElse(false))
                    );

            try (final Response getPlacesResponse = c.get("/api/partners/v1/places/"))
            {
                assertThat(getPlacesResponse.isSuccessful()).isTrue();

                final ResponseBody responseBody = getPlacesResponse.body();
                assertThat(responseBody).isNotNull();

                final PlacesTestDto places = jsonMapper.fromJson(responseBody.string(), PlacesTestDto.class);
                assertThat(places.places()).hasSize(1).first().isEqualTo(new PlacesTestDto.PlaceTestDto(placeId, row, seat, true));
            }

            try (final Response getOrderResponse = c.get("/api/partners/v1/orders/" + orderId))
            {
                assertThat(getOrderResponse.isSuccessful()).isTrue();

                final ResponseBody responseBody = getOrderResponse.body();
                assertThat(responseBody).isNotNull();

                final OrderDto order = jsonMapper.fromJson(responseBody.string(), OrderDto.class);
                assertThat(order).isEqualTo(new OrderDto(orderId, OrderStatus.CANCELLED, clocks.now().truncatedTo(ChronoUnit.SECONDS), clocks.now().truncatedTo(ChronoUnit.SECONDS), 0));
            }
        });
    }
}
