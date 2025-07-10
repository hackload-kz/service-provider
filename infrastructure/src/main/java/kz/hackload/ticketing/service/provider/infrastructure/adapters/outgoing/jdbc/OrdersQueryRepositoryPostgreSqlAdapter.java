package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.orders.GetOrderQueryResult;
import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;
import kz.hackload.ticketing.service.provider.domain.orders.OrdersQueryRepository;

public final class OrdersQueryRepositoryPostgreSqlAdapter implements OrdersQueryRepository
{
    private final DataSource dataSource;

    public OrdersQueryRepositoryPostgreSqlAdapter(final DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public Optional<GetOrderQueryResult> getOrder(final OrderId orderId)
    {
        try (final Connection connection = dataSource.getConnection();
             final PreparedStatement statement = connection.prepareStatement("SELECT id, status, places_count, started_at FROM orders WHERE id = ?"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(orderId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);

            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    final OrderId id = new OrderId((UUID) rs.getObject("id"));
                    final OrderStatus status = OrderStatus.valueOf(rs.getString("status"));
                    final long placesCount = rs.getLong("places_count");
                    final Instant startedAt = rs.getObject("started_at", OffsetDateTime.class).toInstant();

                    return Optional.of(new GetOrderQueryResult(id, status, placesCount, startedAt));
                }
                else
                {
                    return Optional.empty();
                }
            }
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }
}
