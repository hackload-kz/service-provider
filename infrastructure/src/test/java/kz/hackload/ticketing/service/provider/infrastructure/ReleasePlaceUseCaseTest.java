package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.util.Optional;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;

import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;

import okhttp3.Response;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.AddPlaceToOrderService;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderNotStartedException;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceAlreadyAddedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceIsNotSelectedException;
import kz.hackload.ticketing.service.provider.domain.orders.PlaceSelectedForAnotherOrderException;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesResourceJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class ReleasePlaceUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;
    private Javalin server;

    private JdbcTransactionManager transactionManager;
    private OrdersRepository ordersRepository;
    private CreatePlaceUseCase createPlaceUseCase;
    private StartOrderUseCase startOrderUseCase;
    private SelectPlaceUseCase selectPlaceUseCase;
    private AddPlaceToOrderUseCase addPlaceToOrderUseCase;

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
        final ReleasePlaceService releasePlaceService = new ReleasePlaceService();
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, placesRepository, ordersRepository);
        addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService);
        final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(releasePlaceService, transactionManager, placesRepository, ordersRepository);

        server = Javalin.create();
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, releasePlaceUseCase);
    }

    @AfterEach
    public void tearDown()
    {
        server.stop();
    }

    @Test
    void shouldReleasePlace() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            OrderNotStartedException,
            PlaceIsNotSelectedException,
            PlaceSelectedForAnotherOrderException,
            PlaceAlreadyAddedException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        selectPlaceUseCase.selectPlaceFor(placeId, orderId);
        addPlaceToOrderUseCase.addPlaceToOrder(placeId, orderId);

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
        });
    }
}
