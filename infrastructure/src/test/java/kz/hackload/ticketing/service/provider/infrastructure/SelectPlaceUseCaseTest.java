package kz.hackload.ticketing.service.provider.infrastructure;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.time.Duration;
import java.util.List;
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

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.application.OutboxScheduler;
import kz.hackload.ticketing.service.provider.application.OutboxSender;
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
import kz.hackload.ticketing.service.provider.domain.orders.RemovePlaceFromOrderService;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;
import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceSelectedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlacesResourceJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.KafkaMessagesListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka.PlaceEventsListener;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OrdersRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.OutboxSenderKafkaAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;
import org.testcontainers.shaded.org.awaitility.Awaitility;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
@TestcontainersKafka(mode = ContainerMode.PER_RUN,
        topics = @Topics(value = {"place-events", "order-events"}, reset = Topics.Mode.PER_METHOD)
)
public class SelectPlaceUseCaseTest extends AbstractIntegrationTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;
    @ConnectionKafka
    private KafkaConnection kafkaConnection;

    private Javalin server;

    private JdbcTransactionManager transactionManager;

    private OutboxRepository outboxRepository;
    private PlacesRepository placesRepository;
    private OrdersRepository ordersRepository;

    private CreatePlaceUseCase createPlaceUseCase;
    private StartOrderUseCase startOrderUseCase;

    private OutboxScheduler outboxScheduler;
    private KafkaMessagesListener kafkaMessagesListener;

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

        outboxRepository = new OutboxRepositoryPostgreSqlAdapter(transactionManager);
        placesRepository = new PlacesRepositoryPostgreSqlAdapter(transactionManager);
        ordersRepository = new OrdersRepositoryPostgreSqlAdapter(transactionManager);
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
        startOrderUseCase = new StartOrderApplicationService(transactionManager, ordersRepository);

        final SelectPlaceService selectPlaceService = new SelectPlaceService();
        final RemovePlaceFromOrderService removePlaceFromOrderService = new RemovePlaceFromOrderService();
        final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, transactionManager, jsonMapper, placesRepository, ordersRepository, outboxRepository);
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

        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService();
        final AddPlaceToOrderApplicationService addPlaceToOrderApplicationService = new AddPlaceToOrderApplicationService(transactionManager, ordersRepository, placesRepository, addPlaceToOrderService);
        final PlaceEventsListener placeEventsListener = new PlaceEventsListener(jsonMapper, addPlaceToOrderApplicationService);

        final KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        kafkaMessagesListener = new KafkaMessagesListener(consumer, "place-events", placeEventsListener);
        kafkaMessagesListener.start();

        server = Javalin.create();
        new PlacesResourceJavalinHttpAdapter(server, selectPlaceUseCase, removePlaceFromOrderUseCase);
    }

    @AfterEach
    void tearDown()
    {
        outboxScheduler.stop();
        kafkaMessagesListener.stop();
    }

    @Test
    void placeSelected()
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

                Awaitility.await()
                        .atMost(Duration.ofSeconds(60L))
                        .until(() -> transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId)
                                .map(order -> order.contains(placeId))
                                .orElse(false))
                        );

                final Order order = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
                assertThat(order.contains(placeId)).isTrue();
            }
        });
    }
}
