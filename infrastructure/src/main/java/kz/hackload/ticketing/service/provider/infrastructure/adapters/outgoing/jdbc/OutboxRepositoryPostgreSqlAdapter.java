package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessageId;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;

public final class OutboxRepositoryPostgreSqlAdapter implements OutboxRepository
{
    private final JdbcTransactionManager transactionManager;

    public OutboxRepositoryPostgreSqlAdapter(final JdbcTransactionManager transactionManager)
    {
        this.transactionManager = Objects.requireNonNull(transactionManager);
    }

    @Override
    public OutboxMessageId nextId()
    {
        return new OutboxMessageId(UUID.randomUUID());
    }

    @Override
    public void save(final OutboxMessage outboxMessage)
    {
        final Connection connection = transactionManager.currentConnection();

        try (final var statement = connection.prepareStatement("INSERT INTO outbox(id, topic, aggregate_id, occurred_on, aggregate_type, event_type, payload) VALUES (?, ?, ?, ?, ?, ?, ?)"))
        {
            final PGobject idPgObject = new PGobject();
            idPgObject.setValue(outboxMessage.id().value().toString());
            idPgObject.setType("uuid");

            statement.setObject(1, idPgObject);
            statement.setString(2, outboxMessage.topic());
            statement.setString(3, outboxMessage.aggregateId());
            statement.setObject(4, outboxMessage.occurredOn().atOffset(ZoneOffset.UTC));
            statement.setString(5, outboxMessage.aggregateType());
            statement.setString(6, outboxMessage.eventType());

            final PGobject pGobject = new PGobject();
            pGobject.setType("jsonb");
            pGobject.setValue(outboxMessage.payload());

            statement.setObject(7, pGobject);

            statement.execute();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }

    @Override
    public Optional<OutboxMessage> nextForDelivery()
    {
        final Connection connection = transactionManager.currentConnection();

        try (final var statement = connection.prepareStatement("SELECT * FROM outbox ORDER BY occurred_on FOR UPDATE LIMIT 1"))
        {
            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    final UUID id = (UUID) rs.getObject("id");
                    final String topic = rs.getString("topic");
                    final String aggregateId = rs.getString("aggregate_id");
                    final String aggregateType = rs.getString("aggregate_type");
                    final Instant occurredOn = rs.getObject("occurred_on", OffsetDateTime.class).toInstant();
                    final String eventType = rs.getString("event_type");

                    final PGobject pGobject = (PGobject) rs.getObject("payload");
                    final String payload = pGobject.getValue();

                    return Optional.of(new OutboxMessage(new OutboxMessageId(id), topic, aggregateId, occurredOn, aggregateType, eventType, payload));
                }
                else
                {
                    return Optional.empty();
                }
            }
        }
        catch (final SQLException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void delete(final OutboxMessageId id)
    {
        final Connection connection = transactionManager.currentConnection();

        try (final var statement = connection.prepareStatement("DELETE FROM outbox WHERE id = ?"))
        {
            final PGobject idPgObject = new PGobject();
            idPgObject.setValue(id.value().toString());
            idPgObject.setType("uuid");

            statement.setObject(1, idPgObject);

            statement.execute();
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }
}
