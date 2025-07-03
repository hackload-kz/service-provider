package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import kz.hackload.ticketing.service.provider.application.*;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;
import okhttp3.Response;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class ConfirmOrderUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private JdbcTransactionManager transactionManager;
    private OrdersRepository ordersRepository;
    private CreatePlaceUseCase createPlaceUseCase;
    private StartOrderUseCase startOrderUseCase;
    private SelectPlaceUseCase selectPlaceUseCase;
    private SubmitOrderUseCase submitOrderUseCase;
    private AddPlaceToOrderUseCase addPlaceToOrderUseCase;
    private Javalin server;

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

        final PlacesRepository placesRepository = new PlacesRepositoryPostgreSqlAdapter(transactionManager);
        final SelectPlaceService selectPlaceService = new SelectPlaceService();
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, placesRepository, ordersRepository);
        submitOrderUseCase = new SubmitOrderApplicationService(transactionManager, ordersRepository);
        addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService);
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
    void orderConfirmed() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);
        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);
        submitOrderUseCase.submit(orderId);

        JavalinTest.test(server, (_, c) ->
        {
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
        });
    }
}
