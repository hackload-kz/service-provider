package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import kz.hackload.ticketing.service.provider.domain.AggregateRestoreException;
import kz.hackload.ticketing.service.provider.domain.orders.Order;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersRepository;
import kz.hackload.ticketing.service.provider.domain.places.*;

public class PlacesRepositoryJdbcAdapterTest
{
    private final OrdersRepository ordersRepository = new OrdersRepository()
    {
        @Override
        public OrderId nextId()
        {
            return new OrderId(UUID.randomUUID());
        }

        @Override
        public Optional<Order> findById(final OrderId id)
        {
            return Optional.empty();
        }

        @Override
        public void save(final Order order)
        {

        }
    };

    @Test
    void shouldCreatePlace() throws AggregateRestoreException, PlaceAlreadySelectedException
    {
        final HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:postgresql://localhost:5432/hackload_ticketing_sp");
        hikariConfig.setUsername("hackload_ticketing_sp");
        hikariConfig.setPassword("hackload_ticketing_sp");

        final DataSource dataSource = new HikariDataSource(hikariConfig);

        final PlacesRepository placesRepository = new PlacesRepositoryJdbcAdapter(dataSource);

        final Row row = new Row(1);
        final Seat seat = new Seat(1);

        final PlaceId placeId = placesRepository.nextId();
        final Place place = Place.create(placeId, row, seat);

        placesRepository.save(place);

        final Place createdPlace = placesRepository.findById(placeId).orElseThrow();
        final Place createdPlace1 = placesRepository.findById(placeId).orElseThrow();
        assertThat(createdPlace).isEqualTo(place);
        assertThat(createdPlace.isFree()).isTrue();
        assertThat(createdPlace.selectedFor()).isEmpty();
        assertThat(createdPlace.row()).isEqualTo(row);
        assertThat(createdPlace.seat()).isEqualTo(seat);

        final OrderId orderId = ordersRepository.nextId();
        createdPlace.selectFor(orderId);

        placesRepository.save(createdPlace);

        final Place selectedPlace = placesRepository.findById(placeId).orElseThrow();
        assertThat(selectedPlace.isFree()).isFalse();
        assertThat(selectedPlace.selectedFor()).isPresent().get().isEqualTo(orderId);
        assertThat(selectedPlace.row()).isEqualTo(row);
        assertThat(selectedPlace.seat()).isEqualTo(seat);

        createdPlace1.selectFor(orderId);
        placesRepository.save(createdPlace1);
    }
}
