package kz.hackload.ticketing.service.provider.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.util.Properties;
import java.util.UUID;

import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import io.goodforgod.testcontainers.extensions.kafka.ConnectionKafka;
import io.goodforgod.testcontainers.extensions.kafka.KafkaConnection;
import io.goodforgod.testcontainers.extensions.kafka.TestcontainersKafka;
import io.goodforgod.testcontainers.extensions.kafka.Topics;

import io.javalin.Javalin;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

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
import kz.hackload.ticketing.service.provider.application.EventsDispatcher;
import kz.hackload.ticketing.service.provider.application.GetOrderUseCase;
import kz.hackload.ticketing.service.provider.application.GetPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.OrderCancelledEventsHandler;
import kz.hackload.ticketing.service.provider.application.OrdersProjectionService;
import kz.hackload.ticketing.service.provider.application.OrdersQueryService;
import kz.hackload.ticketing.service.provider.application.OutboxScheduler;
import kz.hackload.ticketing.service.provider.application.OutboxSender;
import kz.hackload.ticketing.service.provider.application.PlacesProjectionService;
import kz.hackload.ticketing.service.provider.application.PlacesQueryService;
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
import kz.hackload.ticketing.service.provider.domain.orders.OrdersProjectionsRepository;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersQueryRepository;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlacesProjectionsRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlacesQueryRepository;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.JacksonJsonMapper;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.KafkaMessagesListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.OrderEventsListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.PlaceEventsListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersProjectionsRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersQueryRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesProjectionsRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesQueryRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.kafka.OutboxSenderKafkaAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
@TestcontainersKafka(mode = ContainerMode.PER_RUN,
        topics = @Topics(value = {"place-events", "order-events"}, reset = Topics.Mode.PER_METHOD)
)
public abstract class AbstractIntegrationTest
{
    @ConnectionPostgreSQL
    protected JdbcConnection postgresConnection;
    @ConnectionKafka
    protected KafkaConnection kafkaConnection;

    protected Javalin server;
    protected JsonMapper jsonMapper = new JacksonJsonMapper();
    protected JdbcTransactionManager transactionManager;
    protected FakeClock clocks = new FakeClock();

    protected OrdersRepository ordersRepository;
    protected OutboxRepository outboxRepository;
    protected PlacesRepository placesRepository;
    protected OrdersQueryRepository ordersQueryRepository;
    protected PlacesQueryRepository placesQueryRepository;
    protected OrdersProjectionsRepository ordersProjectionsRepository;
    protected PlacesProjectionsRepository placesProjectionsRepository;

    protected CreatePlaceUseCase createPlaceUseCase;
    protected StartOrderUseCase startOrderUseCase;
    protected SelectPlaceUseCase selectPlaceUseCase;
    protected ReleasePlaceUseCase releasePlaceUseCase;
    protected SubmitOrderUseCase submitOrderUseCase;
    protected AddPlaceToOrderUseCase addPlaceToOrderUseCase;
    protected RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase;
    protected ConfirmOrderUseCase confirmOrderUseCase;
    protected CancelOrderUseCase cancelOrderUseCase;
    protected GetOrderUseCase getOrderUseCase;
    protected GetPlaceUseCase getPlaceUseCase;

    protected OutboxScheduler outboxScheduler;
    private KafkaMessagesListener placeEventskafkaMessagesListener;
    private KafkaMessagesListener orderEventsKafkaListener;

    @BeforeEach
    public void globalSetUp()
    {
        // TODO: somehow use db-migrations
        postgresConnection.execute("""
                create table public.events
                (
                    aggregate_id uuid   not null,
                    revision     bigint not null,
                    event_date   timestamp with time zone not null,
                    event_type   varchar(255) not null,
                    data         jsonb not null,
                    primary key (aggregate_id, revision)
                );

                create table public.outbox
                (
                    id                 uuid primary key,
                    topic              varchar(255) not null,
                    aggregate_id       varchar(255) not null,
                    aggregate_type     varchar(255) not null,
                    aggregate_revision int          not null,
                    event_type         varchar(255) not null,
                    payload            jsonb        not null
                );

                create index idx_aggregate_type_revision on public.outbox (aggregate_type, aggregate_revision);

                create table public.orders
                (
                    id           uuid primary key         not null,
                    status       varchar(255)             not null,
                    places_count int                      not null,
                    started_at   timestamp with time zone not null,
                    updated_at    timestamp with time zone not null,
                    revision     bigint                   not null
                );

                create table places
                (
                    id      uuid primary key not null,
                    row     int              not null,
                    seat    int              not null,
                    is_free bool             not null
                );
                """
        );

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgresConnection.params().jdbcUrl());
        hikariConfig.setUsername(postgresConnection.params().username());
        hikariConfig.setPassword(postgresConnection.params().password());
        hikariConfig.setMaximumPoolSize(20);

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        transactionManager = new JdbcTransactionManager(dataSource);
        ordersRepository = new OrdersRepositoryPostgreSqlAdapter(transactionManager);
        outboxRepository = new OutboxRepositoryPostgreSqlAdapter(transactionManager);
        placesRepository = new PlacesRepositoryPostgreSqlAdapter(transactionManager);
        ordersQueryRepository = new OrdersQueryRepositoryPostgreSqlAdapter(dataSource);
        placesQueryRepository = new PlacesQueryRepositoryPostgreSqlAdapter(dataSource);
        ordersProjectionsRepository = new OrdersProjectionsRepositoryPostgreSqlAdapter(dataSource);
        placesProjectionsRepository = new PlacesProjectionsRepositoryPostgreSqlAdapter(dataSource);

        final SelectPlaceService selectPlaceService = new SelectPlaceService(clocks);
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService(clocks);
        final ReleasePlaceService releasePlaceService = new ReleasePlaceService(clocks);
        final RemovePlaceFromOrderService removePlaceFromOrderService = new RemovePlaceFromOrderService(clocks);

        final EventsDispatcher eventsDispatcher = new EventsDispatcher(jsonMapper, outboxRepository);

        createPlaceUseCase = new CreatePlaceApplicationService(clocks, transactionManager, placesRepository, eventsDispatcher);
        startOrderUseCase = new StartOrderApplicationService(clocks, transactionManager, ordersRepository, eventsDispatcher);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, placesRepository, ordersRepository, eventsDispatcher);
        releasePlaceUseCase = new ReleasePlaceApplicationService(transactionManager, ordersRepository, placesRepository, releasePlaceService, eventsDispatcher);
        submitOrderUseCase = new SubmitOrderApplicationService(clocks, transactionManager, ordersRepository, eventsDispatcher);
        addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService, eventsDispatcher);
        removePlaceFromOrderUseCase = new RemovePlaceFromOrderFromOrderApplicationService(transactionManager, placesRepository, ordersRepository, eventsDispatcher, removePlaceFromOrderService);
        confirmOrderUseCase = new ConfirmOrderApplicationService(clocks, transactionManager, ordersRepository, eventsDispatcher);
        cancelOrderUseCase = new CancelOrderApplicationService(clocks, transactionManager, ordersRepository, eventsDispatcher);
        getOrderUseCase = new OrdersQueryService(ordersQueryRepository);
        getPlaceUseCase = new PlacesQueryService(placesQueryRepository);

        final Properties properties = new Properties();
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection.params().bootstrapServers());
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        final KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        final OutboxSender outboxSender = new OutboxSenderKafkaAdapter(producer);
        outboxScheduler = new OutboxScheduler(transactionManager, outboxRepository, outboxSender);

        final PlacesProjectionService placesProjectionService = new PlacesProjectionService(placesProjectionsRepository);
        final PlaceEventsListener placeEventsListener = new PlaceEventsListener(jsonMapper, placesProjectionService, addPlaceToOrderUseCase);
        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        placeEventskafkaMessagesListener = new KafkaMessagesListener(consumer, "place-events", placeEventsListener);

        final OrdersProjectionService ordersProjectionService = new OrdersProjectionService(ordersProjectionsRepository);
        final OrderCancelledEventsHandler orderCancelledEventsHandler = new OrderCancelledEventsHandler(ordersProjectionService, releasePlaceUseCase);
        final OrderEventsListener orderEventsListener = new OrderEventsListener(jsonMapper, orderCancelledEventsHandler, ordersProjectionService, releasePlaceUseCase);
        final KafkaConsumer<String, String> orderEventsKafkaConsumer = new KafkaConsumer<>(properties);
        orderEventsKafkaListener = new KafkaMessagesListener(orderEventsKafkaConsumer, "order-events", orderEventsListener);

        placeEventskafkaMessagesListener.start();
        orderEventsKafkaListener.start();
        outboxScheduler.start();
        server = Javalin.create();
    }

    @AfterEach
    public void globalTearDown()
    {
        server.stop();
        placeEventskafkaMessagesListener.stop();
        orderEventsKafkaListener.stop();
        outboxScheduler.stop();
    }
}
