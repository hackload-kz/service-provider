package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.Map;
import java.util.UUID;

import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;

public class StartOrderUseCaseTest extends AbstractIntegrationTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp()
    {
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase);
    }

    @Test
    void orderStarted()
    {
        // given

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
                    final Map<?, ?> startedOrderDto = MAPPER.readValue(body.string(), Map.class);
                    final OrderId orderId = new OrderId(UUID.fromString((String) startedOrderDto.get("order_id")));

                    final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
                    assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
                    assertThat(order.places()).isEmpty();
                    assertThat(order.uncommittedEvents()).isEmpty();
                }
            }
        });
    }
}
