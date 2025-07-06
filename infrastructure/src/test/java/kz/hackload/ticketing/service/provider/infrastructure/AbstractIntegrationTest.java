package kz.hackload.ticketing.service.provider.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;

import io.javalin.Javalin;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.CancelOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.CancelOrderUseCase;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderUseCase;
import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderFromOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SubmitOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.SubmitOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.AddPlaceToOrderService;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.JacksonJsonMapper;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public abstract class AbstractIntegrationTest
{
    @ConnectionPostgreSQL
    protected JdbcConnection postgresConnection;

    protected Javalin server;
    protected JsonMapper jsonMapper = new JacksonJsonMapper();
    protected JdbcTransactionManager transactionManager;

    protected OrdersRepository ordersRepository;
    protected OutboxRepository outboxRepository;
    protected PlacesRepository placesRepository;

    protected CreatePlaceUseCase createPlaceUseCase;
    protected StartOrderUseCase startOrderUseCase;
    protected SelectPlaceUseCase selectPlaceUseCase;
    protected ReleasePlaceUseCase releasePlaceUseCase;
    protected SubmitOrderUseCase submitOrderUseCase;
    protected AddPlaceToOrderUseCase addPlaceToOrderUseCase;
    protected RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase;
    protected ConfirmOrderUseCase confirmOrderUseCase;
    protected CancelOrderUseCase cancelOrderUseCase;

    @BeforeEach
    public void globalSetUp()
    {
        postgresConnection.execute("""
                create table public.events
                (
                    aggregate_id uuid not null,
                    revision     bigint       not null,
                    event_type   varchar(255),
                    data         jsonb,
                    primary key (aggregate_id, revision)
                );

                create table public.outbox
                (
                    id              uuid primary key,
                    topic           varchar(255),
                    aggregate_id    varchar(255),
                    aggregate_type  varchar(255),
                    payload         jsonb
                );
                """
        );

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgresConnection.params().jdbcUrl());
        hikariConfig.setUsername(postgresConnection.params().username());
        hikariConfig.setPassword(postgresConnection.params().password());

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        transactionManager = new JdbcTransactionManager(dataSource);
        ordersRepository = new OrdersRepositoryPostgreSqlAdapter(transactionManager);
        outboxRepository = new OutboxRepositoryPostgreSqlAdapter(transactionManager);
        placesRepository = new PlacesRepositoryPostgreSqlAdapter(transactionManager);

        final SelectPlaceService selectPlaceService = new SelectPlaceService();
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
        final ReleasePlaceService releasePlaceService = new ReleasePlaceService();
        final RemovePlaceFromOrderService removePlaceFromOrderService = new RemovePlaceFromOrderService();

        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, jsonMapper, placesRepository, ordersRepository, outboxRepository);
        releasePlaceUseCase = new ReleasePlaceApplicationService(transactionManager, ordersRepository, placesRepository, releasePlaceService);
        submitOrderUseCase = new SubmitOrderApplicationService(transactionManager, ordersRepository);
        addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService);
        removePlaceFromOrderUseCase = new RemovePlaceFromOrderFromOrderApplicationService(jsonMapper, transactionManager, outboxRepository, placesRepository, ordersRepository, removePlaceFromOrderService);
        confirmOrderUseCase = new ConfirmOrderApplicationService(transactionManager, ordersRepository);
        cancelOrderUseCase = new CancelOrderApplicationService(transactionManager, ordersRepository);

        server = Javalin.create();
    }

    @AfterEach
    public void globalTearDown()
    {
        server.stop();
    }
}
