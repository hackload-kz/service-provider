package kz.hackload.ticketing.service.provider.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.time.Duration;
import java.util.Optional;
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
import io.javalin.testtools.JavalinTest;

import okhttp3.Response;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.testcontainers.shaded.org.awaitility.Awaitility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.OutboxScheduler;
import kz.hackload.ticketing.service.provider.application.OutboxSender;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.ReleasePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderFromOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.RemovePlaceFromOrderUseCase;
import kz.hackload.ticketing.service.provider.application.SelectPlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.SelectPlaceUseCase;
import kz.hackload.ticketing.service.provider.application.StartOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.StartOrderUseCase;
import kz.hackload.ticketing.service.provider.domain.orders.AddPlaceToOrderService;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.orders.ReleasePlaceService;
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceAlreadySelectedException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCanNotBeAddedToOrderException;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesResourceJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.KafkaMessagesListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.OrderEventsListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.PlaceEventsListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxSenderKafkaAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
@TestcontainersKafka(mode = ContainerMode.PER_RUN,
        topics = @Topics(value = {"place-events", "order-events"}, reset = Topics.Mode.PER_METHOD)
)
public class RemovePlaceFromOrderUseCaseTest extends AbstractIntegrationTest
{
    private static final Logger LOG = LoggerFactory.getLogger(RemovePlaceFromOrderUseCaseTest.class);

    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;
    @ConnectionKafka
    private KafkaConnection kafkaConnection;

    private Javalin server;
    private OutboxScheduler outboxScheduler;
    private KafkaMessagesListener orderEventsKafkaListener;
    private KafkaMessagesListener placeEventsKafkaListener;

    private JdbcTransactionManager transactionManager;

    private OrdersRepository ordersRepository;
    private PlacesRepository placesRepository;
    private OutboxRepository outboxRepository;

    private CreatePlaceUseCase createPlaceUseCase;
    private StartOrderUseCase startOrderUseCase;
    private SelectPlaceUseCase selectPlaceUseCase;

    @BeforeEach
    void setUp()
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
                """);

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
        final RemovePlaceFromOrderService removePlaceFromOrderService = new RemovePlaceFromOrderService();
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);
        selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, jsonMapper, placesRepository, ordersRepository, outboxRepository);
        final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService);
        final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase = new RemovePlaceFromOrderFromOrderApplicationService(jsonMapper, transactionManager, outboxRepository, placesRepository, ordersRepository, removePlaceFromOrderService);

        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConnection.params().bootstrapServers());

        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        properties.put(ProducerConfig.ACKS_CONFIG, "all");

        final KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        final OutboxSender outboxSender = new OutboxSenderKafkaAdapter(producer);
        outboxScheduler = new OutboxScheduler(transactionManager, outboxRepository, outboxSender);
        outboxScheduler.start();

        final ReleasePlaceService releasePlaceService = new ReleasePlaceService();
        final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(transactionManager, ordersRepository, placesRepository, releasePlaceService);

        final OrderEventsListener orderEventsListener = new OrderEventsListener(jsonMapper, releasePlaceUseCase);
        final KafkaConsumer<String, String> orderEventsKafkaConsumer = new KafkaConsumer<>(properties);
        orderEventsKafkaListener = new KafkaMessagesListener(orderEventsKafkaConsumer, "order-events", orderEventsListener);
        orderEventsKafkaListener.start();

        final PlaceEventsListener placeEventsListener = new PlaceEventsListener(jsonMapper, addPlaceToOrderUseCase);
        final KafkaConsumer<String, String> placeEventsKafkaConsumer = new KafkaConsumer<>(properties);
        placeEventsKafkaListener = new KafkaMessagesListener(placeEventsKafkaConsumer, "place-events", placeEventsListener);
        placeEventsKafkaListener.start();

        server = Javalin.create();
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, removePlaceFromOrderUseCase);
    }

    @AfterEach
    public void tearDown()
    {
        server.stop();
        outboxScheduler.stop();
        orderEventsKafkaListener.stop();
        placeEventsKafkaListener.stop();
    }

    @Test
    void placeReleased() throws PlaceCanNotBeAddedToOrderException, PlaceAlreadySelectedException
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        final OrderId orderId = startOrderUseCase.startOrder();

        selectPlaceUseCase.selectPlaceFor(placeId, orderId);

        transactionManager.executeInTransaction(() -> outboxRepository.all())
                .forEach(om -> LOG.info("Found an outbox message: {}", om));

        Awaitility.await()
                .atMost(Duration.ofSeconds(60L))
                .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                        .map(order -> order.contains(placeId))
                        .orElse(false))
                );

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

                Awaitility.await()
                        .atMost(Duration.ofSeconds(60L))
                        .until(() -> transactionManager.executeInTransaction(() -> placesRepository.findById(placeId)
                                .map(Place::isFree)
                                .orElse(false))
                        );

                final Optional<Place> optionalPlace = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId));

                final Place place = assertThat(optionalPlace)
                        .isPresent()
                        .get()
                        .actual();

                assertThat(place.isFree()).isTrue();
                assertThat(place.selectedFor()).isEmpty();
                assertThat(place.isSelectedFor(orderId)).isFalse();
            }
        });
    }
}
