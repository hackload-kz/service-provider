package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersProjectionsRepository;

public final class OrdersProjectionsRepositoryPostgreSqlAdapter implements OrdersProjectionsRepository
{
    private final DataSource dataSource;

    public OrdersProjectionsRepositoryPostgreSqlAdapter(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void insertStartedOrder(final OrderId orderId, final Instant startedAt, final long revision)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("INSERT INTO orders(id, status, places_count, started_at, updated_at, revision) VALUES (?, 'STARTED', 0, ?, ?, ?)"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);
            statement.setObject(2, startedAt.atOffset(ZoneOffset.UTC));
            statement.setObject(3, startedAt.atOffset(ZoneOffset.UTC));
            statement.setLong(4, revision);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public void incrementPlacesCount(final OrderId orderId, final Instant placeAddedAt)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("UPDATE orders SET places_count = places_count + 1, updated_at = ? WHERE id = ?"))
        {
            statement.setObject(1, placeAddedAt.atOffset(ZoneOffset.UTC));

            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(2, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public void decrementPlacesCount(final OrderId orderId, final Instant placeRemovedAt)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("UPDATE orders SET places_count = places_count - 1, updated_at = ? WHERE id = ?"))
        {
            statement.setObject(1, placeRemovedAt.atOffset(ZoneOffset.UTC));

            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(2, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public void orderSubmitted(final OrderId orderId, final Instant submittedAt)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("UPDATE orders SET status = 'SUBMITTED', updated_at = ? WHERE id = ?"))
        {
            statement.setObject(1, submittedAt.atOffset(ZoneOffset.UTC));

            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(2, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public void orderConfirmed(final OrderId orderId, final Instant orderConfirmed)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("UPDATE orders SET status = 'CONFIRMED', updated_at = ? WHERE id = ?"))
        {
            statement.setObject(1, orderConfirmed.atOffset(ZoneOffset.UTC));

            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(2, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }
}
