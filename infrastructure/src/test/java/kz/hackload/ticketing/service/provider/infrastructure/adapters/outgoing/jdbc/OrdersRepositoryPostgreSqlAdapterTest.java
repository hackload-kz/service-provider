package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.goodforgod.testcontainers.extensions.ContainerMode;
import io.goodforgod.testcontainers.extensions.jdbc.ConnectionPostgreSQL;
import io.goodforgod.testcontainers.extensions.jdbc.JdbcConnection;
import io.goodforgod.testcontainers.extensions.jdbc.TestcontainersPostgreSQL;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.PlacesRepositoryInMemoryAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_RUN)
public class OrdersRepositoryPostgreSqlAdapterTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;
    private final PlacesRepository placesRepository = new PlacesRepositoryInMemoryAdapter();
    private JdbcTransactionManager transactionManager;
    private OrdersRepository ordersRepository;

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
    }

    @AfterEach
    void tearDown()
    {
        postgresConnection.execute("DROP TABLE public.events;");
    }

    @Test
    void shouldSaveStartedOrder()
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order foundOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        assertThat(foundOrder.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(foundOrder.places()).isEmpty();
        assertThat(foundOrder.canAddPlace()).isTrue();
        assertThat(foundOrder.revision()).isEqualTo(1);
    }

    @Test
    void shouldSaveOrderAfterAddingPlace() throws OrderNotStartedException, PlaceAlreadyAddedException
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order startedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final PlaceId placeId = placesRepository.nextId();

        startedOrder.addPlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(startedOrder));

        final Order orderWithPlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        assertThat(orderWithPlace.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(orderWithPlace.places()).hasSize(1).first().isEqualTo(placeId);
        assertThat(orderWithPlace.canAddPlace()).isTrue();
        assertThat(orderWithPlace.contains(placeId)).isTrue();
        assertThat(orderWithPlace.revision()).isEqualTo(2);
    }

    @Test
    void shouldSaveOrderAfterRemovePlace() throws OrderNotStartedException, PlaceAlreadyAddedException, PlaceNotAddedException
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order startedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final PlaceId placeId = placesRepository.nextId();

        startedOrder.addPlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(startedOrder));

        final Order orderWithPlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        orderWithPlace.removePlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(orderWithPlace));

        final Order orderWithRemovePlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        assertThat(orderWithRemovePlace.status()).isEqualTo(OrderStatus.STARTED);
        assertThat(orderWithRemovePlace.places()).isEmpty();
        assertThat(orderWithRemovePlace.canAddPlace()).isTrue();
        assertThat(orderWithRemovePlace.contains(placeId)).isFalse();
        assertThat(orderWithRemovePlace.revision()).isEqualTo(3);
    }

    @Test
    void shouldSaveSubmittedOrder() throws OrderNotStartedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order startedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final PlaceId placeId = placesRepository.nextId();

        startedOrder.addPlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(startedOrder));

        final Order orderWithPlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        orderWithPlace.submit();
        transactionManager.executeInTransaction(() -> ordersRepository.save(orderWithPlace));

        final Order submittedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());

        assertThat(submittedOrder.status()).isEqualTo(OrderStatus.SUBMITTED);
        assertThat(submittedOrder.places()).hasSize(1).first().isEqualTo(placeId);
        assertThat(submittedOrder.canAddPlace()).isFalse();
        assertThat(submittedOrder.contains(placeId)).isTrue();
        assertThat(submittedOrder.revision()).isEqualTo(3);
    }

    @Test
    void shouldSaveConfirmedOrder() throws OrderNotStartedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException,
            OrderNotSubmittedException
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order startedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final PlaceId placeId = placesRepository.nextId();

        startedOrder.addPlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(startedOrder));

        final Order orderWithPlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        orderWithPlace.submit();
        transactionManager.executeInTransaction(() -> ordersRepository.save(orderWithPlace));

        final Order submittedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        submittedOrder.confirm();
        transactionManager.executeInTransaction(() -> ordersRepository.save(submittedOrder));

        final Order confirmedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        assertThat(confirmedOrder.status()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmedOrder.places()).hasSize(1).first().isEqualTo(placeId);
        assertThat(confirmedOrder.canAddPlace()).isFalse();
        assertThat(confirmedOrder.contains(placeId)).isTrue();
        assertThat(confirmedOrder.revision()).isEqualTo(4);
    }

    @Test
    void shouldSaveCancelledOrder() throws OrderNotStartedException,
            PlaceAlreadyAddedException,
            NoPlacesAddedException,
            OrderAlreadyCancelledException
    {
        final OrderId orderId = ordersRepository.nextId();

        final Order order = Order.start(orderId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(order));

        final Order startedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        final PlaceId placeId = placesRepository.nextId();

        startedOrder.addPlace(placeId);
        transactionManager.executeInTransaction(() -> ordersRepository.save(startedOrder));

        final Order orderWithPlace = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        orderWithPlace.submit();
        transactionManager.executeInTransaction(() -> ordersRepository.save(orderWithPlace));

        final Order submittedOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        submittedOrder.cancel();
        transactionManager.executeInTransaction(() -> ordersRepository.save(submittedOrder));

        final Order cancelledOrder = transactionManager.executeInTransaction(() -> ordersRepository.findById(orderId).orElseThrow());
        assertThat(cancelledOrder.status()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(cancelledOrder.places()).isEmpty();
        assertThat(cancelledOrder.canAddPlace()).isFalse();
        assertThat(cancelledOrder.contains(placeId)).isFalse();
        assertThat(cancelledOrder.revision()).isEqualTo(4);
    }
}
