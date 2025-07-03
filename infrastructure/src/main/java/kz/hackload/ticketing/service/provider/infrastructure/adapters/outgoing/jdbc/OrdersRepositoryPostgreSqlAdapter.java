package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import kz.hackload.ticketing.service.provider.domain.orders.*;
import org.postgresql.util.PGobject;

public final class OrdersRepositoryPostgreSqlAdapter implements OrdersRepository
{
    private final JdbcTransactionManager transactionManager;
    // todo: replace with constructor injection
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OrdersRepositoryPostgreSqlAdapter(final JdbcTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    @Override
    public OrderId nextId()
    {
        return new OrderId(UUID.randomUUID());
    }

    @Override
    public Optional<Order> findById(final OrderId orderId)
    {
        record ResultSetRow(String id, String eventType, long revision, String data) {}

        final ArrayList<ResultSetRow> rsRows = new ArrayList<>();

        final Connection connection = transactionManager.currentConnection();
        try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM events WHERE aggregate_id = ?"))
        {
            statement.setString(1, orderId.value().toString());
            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    do
                    {
                        final String id = rs.getString("aggregate_id");
                        final String eventType = rs.getString("event_type");
                        final long revision = rs.getLong("revision");

                        final PGobject pGobject = (PGobject) rs.getObject("data");
                        final String data = pGobject.getValue();

                        rsRows.add(new ResultSetRow(id, eventType, revision, data));
                    }
                    while (rs.next());
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

        final List<OrderDomainEvent> events = new ArrayList<>(rsRows.size());

        try
        {
            for (final ResultSetRow row : rsRows)
            {
                final OrderDomainEvent event = switch (row.eventType)
                {
                    case "order_started_event" -> objectMapper.readValue(row.data, OrderStartedEvent.class);
                    case "place_added_to_order_event" -> objectMapper.readValue(row.data, PlaceAddedToOrderEvent.class);
                    case "place_removed_from_order_event" -> objectMapper.readValue(row.data, PlaceRemovedFromOrderEvent.class);
                    case "order_submitted_event" -> objectMapper.readValue(row.data, OrderSubmittedEvent.class);
                    case "order_confirmed_event" -> objectMapper.readValue(row.data, OrderConfirmedEvent.class);
                    case "order_cancelled_event" -> objectMapper.readValue(row.data, OrderCancelledEvent.class);
                    default -> throw new RuntimeException("Unknown event type: " + row.eventType);
                };

                events.add(event);
            }
        }
        catch (final JsonProcessingException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }

        return Optional.of(Order.restore(orderId, events.size(), events));
    }

    @Override
    public void save(final Order order)
    {
        final String id = order.id().value().toString();
        final List<OrderDomainEvent> uncommittedEvents = order.uncommittedEvents();
        final Map<OrderDomainEvent, String> uncommittedEventToJsonMap = new HashMap<>(uncommittedEvents.size());
        for (final OrderDomainEvent event : uncommittedEvents)
        {
            try
            {
                final String json = objectMapper.writeValueAsString(event);
                uncommittedEventToJsonMap.put(event, json);
            }
            catch (JsonProcessingException e)
            {
                // todo: replace with domain exception
                throw new RuntimeException(e);
            }
        }

        // todo: rename db column to revision
        long currentRevision = order.revision();

        final Connection connection = transactionManager.currentConnection();
        try (final var statement = connection.prepareStatement("INSERT INTO events(aggregate_id, event_type, revision, data) VALUES (?, ?, ?, ?)"))
        {
            for (final var entry : uncommittedEventToJsonMap.entrySet())
            {
                statement.setString(1, id);
                statement.setString(2, entry.getKey().type());
                statement.setLong(3, currentRevision++);

                final PGobject pGobject = new PGobject();
                pGobject.setType("jsonb");
                pGobject.setValue(entry.getValue());

                statement.setObject(4, pGobject);
                statement.addBatch();
            }

            statement.execute();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }
}
