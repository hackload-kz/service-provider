package kz.hackload.ticketing.service.provider.infrastructure;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.time.Duration;
import java.util.Properties;

import io.javalin.Javalin;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
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
import kz.hackload.ticketing.service.provider.domain.Clocks;
import kz.hackload.ticketing.service.provider.domain.RealClock;
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
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.OrderResourcesJavalinHttpAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.PlaceResourceJavalinHttpAdapter;
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

public final class ApplicationRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRunner.class);

    @WithSpan(kind = SpanKind.INTERNAL)
    public static void main(String[] args) throws InterruptedException
    {
        final long startNano = System.nanoTime();

        // TODO: extract to env variables
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(System.getenv("DB_JDBC_URL"));
        hikariConfig.setUsername(System.getenv("DB_JDBC_USER"));
        hikariConfig.setPassword(System.getenv("DB_JDBC_PASSWORD"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(System.getenv("DB_CONNECTION_POOL_SIZE")));

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        final HikariConfig projectionsHikariConfig = new HikariConfig();
        projectionsHikariConfig.setJdbcUrl(System.getenv("PROJECTIONS_DB_JDBC_URL"));
        projectionsHikariConfig.setUsername(System.getenv("PROJECTIONS_DB_JDBC_USER"));
        projectionsHikariConfig.setPassword(System.getenv("PROJECTIONS_DB_JDBC_PASSWORD"));
        projectionsHikariConfig.setMaximumPoolSize(Integer.parseInt(System.getenv("PROJECTIONS_DB_CONNECTION_POOL_SIZE")));

        final DataSource projectionsDataSource = new HikariDataSource(projectionsHikariConfig);

        final JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);

        final OrdersRepository ordersRepository = new OrdersRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final PlacesRepository placesRepository = new PlacesRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final OutboxRepository outboxRepository = new OutboxRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final OrdersQueryRepository ordersQueryRepository = new OrdersQueryRepositoryPostgreSqlAdapter(dataSource);
        final PlacesQueryRepository placesQueryRepository = new PlacesQueryRepositoryPostgreSqlAdapter(dataSource);
        final OrdersProjectionsRepository ordersProjectionsRepository = new OrdersProjectionsRepositoryPostgreSqlAdapter(projectionsDataSource);
        final PlacesProjectionsRepository placesProjectionsRepository = new PlacesProjectionsRepositoryPostgreSqlAdapter(projectionsDataSource);

        final Clocks clocks = new RealClock();
        final JsonMapper jsonMapper = new JacksonJsonMapper();
        final EventsDispatcher eventsDispatcher = new EventsDispatcher(jsonMapper, outboxRepository);

        final StartOrderUseCase startOrderUseCase = new StartOrderApplicationService(clocks, jdbcTransactionManager, ordersRepository, eventsDispatcher);
        final SubmitOrderUseCase submitOrderUseCase = new SubmitOrderApplicationService(clocks, jdbcTransactionManager, ordersRepository, eventsDispatcher);
        final ConfirmOrderUseCase confirmOrderUseCase = new ConfirmOrderApplicationService(clocks, jdbcTransactionManager, ordersRepository, eventsDispatcher);
        final CancelOrderUseCase cancelOrderUseCase = new CancelOrderApplicationService(clocks, jdbcTransactionManager, ordersRepository, eventsDispatcher);

        final SelectPlaceService selectPlaceService = new SelectPlaceService(clocks);
        final RemovePlaceFromOrderService removePlaceFromOrderService = new RemovePlaceFromOrderService(clocks);
        final AddPlaceToOrderService addPlaceToOrderService = new AddPlaceToOrderService(clocks);
        final ReleasePlaceService releasePlaceService = new ReleasePlaceService(clocks);

        final CreatePlaceUseCase createPlaceUseCase = new CreatePlaceApplicationService(clocks, jdbcTransactionManager, placesRepository, eventsDispatcher);
        final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, jdbcTransactionManager, placesRepository, ordersRepository, eventsDispatcher);
        final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase = new RemovePlaceFromOrderFromOrderApplicationService(jdbcTransactionManager, placesRepository, ordersRepository, eventsDispatcher, removePlaceFromOrderService);
        final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(jdbcTransactionManager, ordersRepository, placesRepository, releasePlaceService, eventsDispatcher);
        final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(jdbcTransactionManager, ordersRepository, placesRepository, addPlaceToOrderService, eventsDispatcher);
        final GetOrderUseCase getOrderUseCase = new OrdersQueryService(ordersQueryRepository);
        final GetPlaceUseCase getPlaceUseCase = new PlacesQueryService(placesQueryRepository);

        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, System.getenv("KAFKA_CONSUMER_GROUP_ID"));
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, System.getenv("KAFKA_CONSUMER_OFFSET"));
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "false");

        final PlacesProjectionService placesProjectionService = new PlacesProjectionService(placesProjectionsRepository);
        final KafkaConsumer<String, String> placeEventsKafkaConsumer = new KafkaConsumer<>(properties);
        final PlaceEventsListener placeEventsListener = new PlaceEventsListener(jsonMapper, placesProjectionService, addPlaceToOrderUseCase);
        final KafkaMessagesListener placeEventsKafkaMessagesListener = new KafkaMessagesListener(placeEventsKafkaConsumer, "place-events", placeEventsListener);
        placeEventsKafkaMessagesListener.start();

        final OrdersProjectionService ordersProjectionService = new OrdersProjectionService(ordersProjectionsRepository);
        final OrderCancelledEventsHandler orderCancelledEventsHandler = new OrderCancelledEventsHandler(ordersProjectionService, releasePlaceUseCase);
        final KafkaConsumer<String, String> orderEventsKafkaConsumer = new KafkaConsumer<>(properties);
        final OrderEventsListener orderEventsListener = new OrderEventsListener(jsonMapper, orderCancelledEventsHandler, ordersProjectionService, releasePlaceUseCase);
        final KafkaMessagesListener orderEventsKafkaMessagesListener = new KafkaMessagesListener(orderEventsKafkaConsumer, "order-events", orderEventsListener);
        orderEventsKafkaMessagesListener.start();

        final KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        final OutboxSender outboxSender = new OutboxSenderKafkaAdapter(kafkaProducer);
        final OutboxScheduler outboxScheduler = new OutboxScheduler(jdbcTransactionManager, outboxRepository, outboxSender);
        outboxScheduler.start();

        final Javalin httpServer = Javalin.create(config -> config.useVirtualThreads = true);
        new OrderResourcesJavalinHttpAdapter(httpServer, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
        new PlaceResourceJavalinHttpAdapter(httpServer, createPlaceUseCase, selectPlaceUseCase, removePlaceFromOrderUseCase, getPlaceUseCase);

        httpServer.start();

        final Runnable shutdownActions = () ->
        {
            LOG.info("Shutdown initiated");
            outboxScheduler.stop();

            orderEventsKafkaMessagesListener.stop();
            placeEventsKafkaMessagesListener.stop();
            LOG.info("Shutdown completed");
        };

        Runtime.getRuntime().addShutdownHook(new Thread(shutdownActions));

        final long stopNano = System.nanoTime();
        LOG.info("Application started in {} ms", Duration.ofNanos(stopNano - startNano).toMillis());
    }
}
