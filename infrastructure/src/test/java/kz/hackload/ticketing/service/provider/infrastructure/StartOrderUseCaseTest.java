package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.util.UUID;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

import okhttp3.Response;
import okhttp3.ResponseBody;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.application.CancelOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.CancelOrderUseCase;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SubmitOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.SubmitOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class StartOrderUseCaseTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private Javalin server;
    private OrdersRepository ordersRepository;
    private JdbcTransactionManager transactionManager;

    @BeforeEach
    void setUp()
    {
        postgresConnection.execute("""
                create table public.events
                (
                    aggregate_id varchar(255) not null,
                    revision     bigint       not null,
                    event_type   varchar(255),
                    data         jsonb,
                    primary key (aggregate_id, revision)
                );
                """);

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgresConnection.params().jdbcUrl());
        hikariConfig.setUsername(postgresConnection.params().username());
        hikariConfig.setPassword(postgresConnection.params().password());

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        transactionManager = new JdbcTransactionManager(dataSource);
        ordersRepository = new OrdersRepositoryPostgreSqlAdapter(transactionManager);

        final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        final SubmitOrderUseCase submitOrderUseCase = new SubmitOrderApplicationService(transactionManager, ordersRepository);
        final ConfirmOrderUseCase confirmOrderUseCase = new ConfirmOrderApplicationService(transactionManager, ordersRepository);
        final CancelOrderUseCase cancelOrderUseCase = new CancelOrderApplicationService(transactionManager, ordersRepository);

        server = Javalin.create();
        new OrderResourcesJavalinHttpAdapter(server, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase);
    }

    @AfterEach
    void tearDown()
    {
        server.stop();
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
                    final StartedOrderDto startedOrderDto = MAPPER.readValue(body.string(), StartedOrderDto.class);
                    final OrderId orderId = new OrderId(UUID.fromString(startedOrderDto.orderId()));

                    final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
                    assertThat(order.status()).isEqualTo(OrderStatus.STARTED);
                    assertThat(order.places()).isEmpty();
                    assertThat(order.uncommittedEvents()).isEmpty();
                }
            }
        });
    }
}
