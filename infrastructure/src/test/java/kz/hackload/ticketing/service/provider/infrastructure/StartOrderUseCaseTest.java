package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class StartOrderUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private OrdersRepository ordersRepository;
    private JdbcTransactionManager transactionManager;
    private StartOrderUseCase startOrderUseCase;

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
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
    }

    @Test
    void shouldStartOrder()
    {
        final OrderId orderId = startOrderUseCase.startOrder();

        final Optional<Order> optionalOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId));

        final Order actual = assertThat(optionalOrder)
                .isPresent()
                .get()
                .actual();

        assertThat(actual.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(actual.places()).isEmpty();
        assertThat(actual.uncommittedEvents()).isEmpty();
    }
}
