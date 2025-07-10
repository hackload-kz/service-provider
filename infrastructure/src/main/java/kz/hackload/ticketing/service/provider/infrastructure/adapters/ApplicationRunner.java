package kz.hackload.ticketing.service.provider.infrastructure.adapters;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

import java.time.Duration;
import java.util.Properties;
import java.util.UUID;

import io.javalin.Javalin;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.AddPlaceToOrderUseCase;
import kz.hackload.ticketing.service.provider.application.CancelOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.CancelOrderUseCase;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderApplicationService;
import kz.hackload.ticketing.service.provider.application.ConfirmOrderUseCase;
import kz.hackload.ticketing.service.provider.application.EventsDispatcher;
import kz.hackload.ticketing.service.provider.application.GetOrderUseCase;
import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.application.OrdersProjectionService;
import kz.hackload.ticketing.service.provider.application.OrdersQueryService;
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
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.domain.places.SelectPlaceService;
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
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.kafka.OutboxSenderKafkaAdapter;

public final class ApplicationRunner
{
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationRunner.class);

    public static void main(String[] args)
    {
        final long startNano = System.nanoTime();

        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/hackload_ticketing_sp");
        hikariConfig.setUsername("hackload_ticketing_sp");
        hikariConfig.setPassword("hackload_ticketing_sp");

        final DataSource dataSource = new HikariDataSource(hikariConfig);
        final JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);

        final OrdersRepository ordersRepository = new OrdersRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final PlacesRepository placesRepository = new PlacesRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final OutboxRepository outboxRepository = new OutboxRepositoryPostgreSqlAdapter(jdbcTransactionManager);
        final OrdersQueryRepository ordersQueryRepository = new OrdersQueryRepositoryPostgreSqlAdapter(dataSource);
        final OrdersProjectionsRepository ordersProjectionsRepository = new OrdersProjectionsRepositoryPostgreSqlAdapter(dataSource);;

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

        final SelectPlaceUseCase selectPlaceUseCase = new SelectPlaceApplicationService(selectPlaceService, jdbcTransactionManager, placesRepository, ordersRepository, eventsDispatcher);
        final RemovePlaceFromOrderUseCase removePlaceFromOrderUseCase = new RemovePlaceFromOrderFromOrderApplicationService(jdbcTransactionManager, placesRepository, ordersRepository, eventsDispatcher, removePlaceFromOrderService);
        final ReleasePlaceUseCase releasePlaceUseCase = new ReleasePlaceApplicationService(jdbcTransactionManager, ordersRepository, placesRepository, releasePlaceService);
        final AddPlaceToOrderUseCase addPlaceToOrderUseCase = new AddPlaceToOrderApplicationService(jdbcTransactionManager, ordersRepository, placesRepository, addPlaceToOrderService, eventsDispatcher);
        final GetOrderUseCase getOrderUseCase = new OrdersQueryService(ordersQueryRepository);

        final Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092,127.0.0.1:9095,127.0.0.1:9098");
        properties.put(ProducerConfig.ACKS_CONFIG, "all");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringSerializer");

        properties.put(ConsumerConfig.GROUP_ID_CONFIG, UUID.randomUUID().toString());
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");

        final KafkaConsumer<String, String> placeEventsKafkaConsumer = new KafkaConsumer<>(properties);
        final PlaceEventsListener placeEventsListener = new PlaceEventsListener(jsonMapper, addPlaceToOrderUseCase);
        final KafkaMessagesListener placeEventsKafkaMessagesListener = new KafkaMessagesListener(placeEventsKafkaConsumer, "place-events", placeEventsListener);
        placeEventsKafkaMessagesListener.start();

        final OrdersProjectionService ordersProjectionService = new OrdersProjectionService(ordersProjectionsRepository);
        final KafkaConsumer<String, String> orderEventsKafkaConsumer = new KafkaConsumer<>(properties);
        final OrderEventsListener orderEventsListener = new OrderEventsListener(jsonMapper, ordersProjectionService, releasePlaceUseCase);
        final KafkaMessagesListener orderEventsKafkaMessagesListener = new KafkaMessagesListener(orderEventsKafkaConsumer, "order-events", orderEventsListener);
        orderEventsKafkaMessagesListener.start();

        final KafkaProducer<String, String> kafkaProducer = new KafkaProducer<>(properties);
        final OutboxSender outboxSender = new OutboxSenderKafkaAdapter(kafkaProducer);
        final OutboxScheduler outboxScheduler = new OutboxScheduler(jdbcTransactionManager, outboxRepository, outboxSender);
        outboxScheduler.start();

        final Javalin httpServer = Javalin.create();
        new OrderResourcesJavalinHttpAdapter(httpServer, startOrderUseCase, submitOrderUseCase, confirmOrderUseCase, cancelOrderUseCase, getOrderUseCase);
        new PlaceResourceJavalinHttpAdapter(httpServer, selectPlaceUseCase, removePlaceFromOrderUseCase);

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
