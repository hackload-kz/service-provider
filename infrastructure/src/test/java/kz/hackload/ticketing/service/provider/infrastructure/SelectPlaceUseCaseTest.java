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
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class SelectPlaceUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private PlacesRepository placesRepository;
    private JdbcTransactionManager transactionManager;
    private CreatePlaceUseCase createPlaceUseCase;
    private SelectPlaceUseCase selectPlaceUseCase;
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

        placesRepository = new PlacesRepositoryPostgreSqlAdapter(transactionManager);
        final OrdersRepository ordersRepository = new OrdersRepositoryPostgreSqlAdapter(transactionManager);
        final SelectPlaceService selectPlaceService = new SelectPlaceService();
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, placesRepository, ordersRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
    }

    @Test
    void shouldSelectPlace() throws PlaceAlreadySelectedException, PlaceCanNotBeAddedToOrderException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        // when
        selectPlaceUseCase.selectPlaceFor(placeId, orderId);

        // then
        final Place place = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(place.isFree()).isFalse();
        assertThat(place.isSelectedFor(orderId)).isTrue();
        assertThat(place.selectedFor()).isPresent().get().isEqualTo(orderId);

//        Optional<OutboxMessage> optionalOutboxMessage = Optional.empty();//outboxRepository.nextForDelivery();
//        OutboxMessage outboxMessage = assertThat(optionalOutboxMessage).isPresent().actual().get();
//        assertThat(outboxMessage.id()).isEqualTo(new OutboxMessageId(1L));
//        assertThat(outboxMessage.aggregateId()).isEqualTo(placeId.value().toString());
//        assertThat(outboxMessage.aggregateType()).isEqualTo("place");
//        assertThat(outboxMessage.payload()).isEqualTo("""
//                {"order_id":"%s"}
//                """.formatted(orderId.value()));
    }
}
