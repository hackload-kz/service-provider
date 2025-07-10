package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesProjectionsRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public final class PlacesProjectionsRepositoryPostgreSqlAdapter implements PlacesProjectionsRepository
{
    private final DataSource dataSource;

    public PlacesProjectionsRepositoryPostgreSqlAdapter(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public void placeCreated(final PlaceId placeId, final Row row, final Seat seat)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("INSERT INTO places(id, row, seat, is_free) VALUES(?, ?, ?, true)"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(placeId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);
            statement.setInt(2, row.number());
            statement.setInt(3, seat.number());

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void placeSelected(final PlaceId placeId)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("update places set is_free = false where id = ?"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(placeId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void placeReleased(final PlaceId placeId)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("update places set is_free = true where id = ?"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(placeId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);

            statement.executeUpdate();
        }
        catch (final SQLException e)
        {
            throw new RuntimeException(e);
        }
    }
}
