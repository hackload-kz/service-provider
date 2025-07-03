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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.places.Place;
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
public class SelectPlaceUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;
    private Javalin server;
    private PlacesRepository placesRepository;
    private JdbcTransactionManager transactionManager;
    private CreatePlaceUseCase createPlaceUseCase;
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
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        final SelectPlaceService selectPlaceService = new SelectPlaceService();
        final ReleasePlaceService releasePlaceService = new ReleasePlaceService();
        final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, placesRepository, ordersRepository);
        final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(releasePlaceService, transactionManager, placesRepository, ordersRepository);

        server = Javalin.create();
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, releasePlaceUseCase);
    }

    @Test
    void shouldSelectPlace()
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        JavalinTest.test(server, (_, c) ->
        {
            // when
            try (final Response response = c.patch("/api/partners/v1/places/" + placeId.value() + "/select", """
                    {
                        "order_id": "%s",
                        "place_id": "%s"
                    }
                    """.formatted(orderId.value(), placeId.value())))
            {
                // then
                assertThat(response.isSuccessful()).isTrue();
                final Optional<Place> optionalPlace = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId));

                final Place actual = assertThat(optionalPlace)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(actual.isFree()).isFalse();
                assertThat(actual.selectedFor()).isPresent().get().isEqualTo(orderId);
                assertThat(actual.isSelectedFor(orderId)).isTrue();

        //        Optional<OutboxMessage> optionalOutboxMessage = Optional.empty();//outboxRepository.nextForDelivery();
        //        OutboxMessage outboxMessage = assertThat(optionalOutboxMessage).isPresent().actual().get();
        //        assertThat(outboxMessage.id()).isEqualTo(new OutboxMessageId(1L));
        //        assertThat(outboxMessage.aggregateId()).isEqualTo(placeId.value().toString());
        //        assertThat(outboxMessage.aggregateType()).isEqualTo("place");
        //        assertThat(outboxMessage.payload()).isEqualTo("""
        //                {"order_id":"%s"}
        //                """.formatted(orderId.value()));
            }
        });
    }
}
