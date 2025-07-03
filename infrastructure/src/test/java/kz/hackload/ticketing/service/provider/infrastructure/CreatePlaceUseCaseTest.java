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
import kz.hackload.ticketing.service.provider.application.CreatePlaceApplicationService;
import kz.hackload.ticketing.service.provider.application.CreatePlaceUseCase;
import kz.hackload.ticketing.service.provider.domain.places.*;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.JdbcTransactionManager;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc.PlacesRepositoryPostgreSqlAdapter;

@TestcontainersPostgreSQL(mode = ContainerMode.PER_METHOD)
public class CreatePlaceUseCaseTest
{
    @ConnectionPostgreSQL
    private JdbcConnection postgresConnection;

    private JdbcTransactionManager transactionManager;
    private PlacesRepository placesRepository;
    private CreatePlaceUseCase createPlaceUseCase;

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
        createPlaceUseCase = new CreatePlaceApplicationService(transactionManager, placesRepository);
    }

    @Test
    void shouldCreatePlace()
    {
        // given
        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        // when
        final PlaceId placeId = createPlaceUseCase.create(row, seat);

        // then
        final Place actual = transactionManager.executeInTransaction(() -> placesRepository.findById(placeId).orElseThrow());
        assertThat(actual.id()).isEqualTo(placeId);
        assertThat(actual.isFree()).isTrue();
        assertThat(actual.selectedFor()).isEmpty();
    }
}
