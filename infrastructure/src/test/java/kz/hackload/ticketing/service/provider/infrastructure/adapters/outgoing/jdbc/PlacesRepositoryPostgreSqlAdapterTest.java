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
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.OrdersRepositoryInMemoryAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class PlacesRepositoryPostgreSqlAdapterTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private final OrdersRepository ordersRepository = new OrdersRepositoryInMemoryAdapter();

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
    }

    @AfterEach
    void tearDown()
    {
        postgresConnection.execute("DROP TABLE public.events;");
    }

    @Test
    void shouldSavePlaceAsEventsInDbThenRestore() throws PlaceAlreadySelectedException, PlaceAlreadyReleasedException
    {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(postgresConnection.params().jdbcUrl());
        hikariConfig.setUsername(postgresConnection.params().username());
        hikariConfig.setPassword(postgresConnection.params().password());

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        final JdbcTransactionManager jdbcTransactionManager = new JdbcTransactionManager(dataSource);

        final PlacesRepository placesRepository = new PlacesRepositoryPostgreSqlAdapter(jdbcTransactionManager);

        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = placesRepository.nextId();
        final Place place = Place.create(placeId, row, seat);

        jdbcTransactionManager.executeInTransaction(() -> placesRepository.save(place));

        final Place createdPlace = jdbcTransactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(createdPlace).isEqualTo(place);
        assertThat(createdPlace.isFree()).isTrue();
        assertThat(createdPlace.selectedFor()).isEmpty();
        assertThat(createdPlace.row()).isEqualTo(row);
        assertThat(createdPlace.seat()).isEqualTo(seat);

        final OrderId orderId = ordersRepository.nextId();
        createdPlace.selectFor(orderId);

        jdbcTransactionManager.executeInTransaction(() -> placesRepository.save(createdPlace));

        final Place selectedPlace = jdbcTransactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(selectedPlace.isFree()).isFalse();
        assertThat(selectedPlace.selectedFor()).isPresent().get().isEqualTo(orderId);
        assertThat(selectedPlace.row()).isEqualTo(row);
        assertThat(selectedPlace.seat()).isEqualTo(seat);

        selectedPlace.release();

        jdbcTransactionManager.executeInTransaction(() -> placesRepository.save(selectedPlace));

        final Place releasedPlace = jdbcTransactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(releasedPlace.isFree()).isTrue();
        assertThat(releasedPlace.selectedFor()).isEmpty();
        assertThat(releasedPlace.row()).isEqualTo(row);
        assertThat(releasedPlace.seat()).isEqualTo(seat);
    }
}
