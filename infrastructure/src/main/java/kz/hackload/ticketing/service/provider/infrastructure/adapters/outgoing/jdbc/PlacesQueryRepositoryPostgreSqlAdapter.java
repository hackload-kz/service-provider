package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlacesQueryRepository;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public final class PlacesQueryRepositoryPostgreSqlAdapter implements PlacesQueryRepository
{
    private final DataSource dataSource;

    public PlacesQueryRepositoryPostgreSqlAdapter(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<GetPlaceQueryResult> getPlace(final PlaceId placeId)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(
                """
                SELECT id, row, seat, is_free 
                FROM places
                WHERE id = ?
                """))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(placeId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);

            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    final PlaceId id = new PlaceId((UUID) rs.getObject("id"));
                    final Row row = new Row(rs.getInt("row"));
                    final Seat seat = new Seat(rs.getInt("seat"));
                    final boolean isFree = rs.getBoolean("is_free");

                    return Optional.of(new GetPlaceQueryResult(id, row, seat, isFree));
                }
                else
                {
                    return Optional.empty();
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<GetPlaceQueryResult> getPlaces(final int page, final int pageSize)
    {
        final List<GetPlaceQueryResult> places = new ArrayList<>(page * pageSize);
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement(
                """
                SELECT 
                    id,
                    row,
                    seat,
                    is_free
                FROM places 
                ORDER BY row, seat
                LIMIT ? 
                OFFSET ?
                """))
        {
            statement.setInt(1, pageSize);
            statement.setInt(2, (page - 1) * pageSize);

            try (final ResultSet rs = statement.executeQuery())
            {
                while (rs.next())
                {
                    final PlaceId id = new PlaceId((UUID) rs.getObject("id"));
                    final Row row = new Row(rs.getInt("row"));
                    final Seat seat = new Seat(rs.getInt("seat"));
                    final boolean isFree = rs.getBoolean("is_free");

                    final GetPlaceQueryResult place = new GetPlaceQueryResult(id, row, seat, isFree);
                    places.add(place);
                }
            }
        }
        catch (SQLException e)
        {
            throw new RuntimeException(e);
        }

        return places;
    }
}
