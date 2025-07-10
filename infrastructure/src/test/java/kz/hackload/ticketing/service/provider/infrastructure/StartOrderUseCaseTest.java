package kz.hackload.ticketing.service.provider.infrastructure;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import io.goodforgod.testcontainers.extensions.kafka.KafkaConnection;
import io.goodforgod.testcontainers.extensions.kafka.ReceivedEvent;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import org.json.JSONObject;

public class StartOrderUseCaseTest extends AbstractIntegrationTest
{
    @BeforeEach
    void setUp()
    {
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
    }

    @Test
    void orderStarted()
    {
        // given
        clocks.setClock(Clock.fixed(Instant.now(), ZoneId.systemDefault()));
        final JSONObject expectedOrderStartedEventInKafka = new JSONObject();
        expectedOrderStartedEventInKafka.accumulate("occurredOn", clocks.now().atOffset(ZoneOffset.UTC));
        expectedOrderStartedEventInKafka.accumulate("revision", 1L);

        JavalinTest.test(server, (_, c) ->
        {
            // when
            try (final Response response = c.post("/api/partners/v1/orders"))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();
                try (final ResponseBody body = response.body())
                {
                    assertThat(body).isNotNull();
                    final Map<?, ?> startedOrderDto = jsonMapper.fromJson(body.string(), Map.class);
                    final OrderId orderId = new OrderId(UUID.fromString((String) startedOrderDto.get("order_id")));

                    Awaitility.await()
                            .atMost(Duration.ofSeconds(60L))
                            .until(() -> ordersQueryRepository.getOrder(orderId).isPresent());

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
                }
            }
        });
    }
}
