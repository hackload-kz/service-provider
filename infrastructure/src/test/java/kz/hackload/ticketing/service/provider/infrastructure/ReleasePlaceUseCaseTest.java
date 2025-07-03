package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import kz.hackload.ticketing.service.provider.application.*;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class ReleasePlaceUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private JdbcTransactionManager transactionManager;
    private OrdersRepository ordersRepository;
    private CreatePlaceUseCase createPlaceUseCase;
    private StartOrderUseCase startOrderUseCase;
    private SelectPlaceUseCase selectPlaceUseCase;
    private AddPlaceToOrderUseCase addPlaceToOrderUseCase;
    private ReleasePlaceUseCase releasePlaceUseCase;

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
        releasePlaceUseCase = new ReleasePlaceApplicationService(releasePlaceService, transactionManager, placesRepository, ordersRepository);
    }

    @Test
    void shouldReleasePlace() throws PlaceCanNotBeAddedToOrderException,
            PlaceAlreadySelectedException,
            PlaceAlreadyReleasedException,
            OrderNotStartedException,
            PlaceNotAddedException,
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

        // when
        releasePlaceUseCase.releasePlace(placeId);

        // then
        final Order result = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        assertThat(result.places()).isEmpty();
        assertThat(result.uncommittedEvents()).isEmpty();
    }
}
