package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.postgresql.util.PGobject;

import kz.hackload.ticketing.service.provider.domain.places.Place;
import kz.hackload.ticketing.service.provider.domain.places.PlaceCreatedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceDomainEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.PlaceReleasedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlaceSelectedEvent;
import kz.hackload.ticketing.service.provider.domain.places.PlacesRepository;

public final class PlacesRepositoryPostgreSqlAdapter implements PlacesRepository
{
    private final JdbcTransactionManager transactionManager;
    // todo: replace with constructor injection
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public PlacesRepositoryPostgreSqlAdapter(final JdbcTransactionManager transactionManager)
    {
        this.transactionManager = transactionManager;
    }

    @Override
    public PlaceId nextId()
    {
        return new PlaceId(UUID.randomUUID());
    }

    @Override
    public void save(final Place place)
    {
        final UUID id = place.id().value();
        final List<PlaceDomainEvent> uncommittedEvents = place.uncommittedEvents();
        final Map<PlaceDomainEvent, String> uncommittedEventToJsonMap = new HashMap<>(uncommittedEvents.size());
        for (final PlaceDomainEvent event : uncommittedEvents)
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
        long currentRevision = place.revision();
        final Connection connection = transactionManager.currentConnection();
        try (final var statement = connection.prepareStatement("INSERT INTO events(aggregate_id, event_type, revision, event_date, data) VALUES (?, ?, ?, ?, ?)"))
        {
            for (final var entry : uncommittedEventToJsonMap.entrySet())
            {
                statement.setObject(1, id, Types.OTHER);
                statement.setString(2, entry.getKey().type());
                statement.setLong(3, currentRevision++);

                statement.setObject(4, entry.getKey().occurredOn().atOffset(ZoneOffset.UTC));

                final PGobject pGobject = new PGobject();
                pGobject.setType("jsonb");
                pGobject.setValue(entry.getValue());

                statement.setObject(5, pGobject);
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

    record ResultSetRow(UUID id, String eventType, long revision, Instant occurredOn, String data)
    {
    }

    @Override
    public Optional<Place> findById(final PlaceId placeId)
    {
        final ArrayList<ResultSetRow> rsRows = new ArrayList<>();

        final Connection connection = transactionManager.currentConnection();
        try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM events WHERE aggregate_id = ?"))
        {
            final PGobject idParamPgObject = new PGobject();
            idParamPgObject.setValue(placeId.value().toString());
            idParamPgObject.setType("uuid");

            statement.setObject(1, idParamPgObject);
            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    do
                    {
                        final UUID id = (UUID) rs.getObject("aggregate_id");
                        final String eventType = rs.getString("event_type");
                        final long revision = rs.getLong("revision");
                        final Instant occurredOn = rs.getObject("event_date", OffsetDateTime.class).toInstant();

                        final PGobject pGobject = (PGobject) rs.getObject("data");
                        final String data = pGobject.getValue();

                        rsRows.add(new ResultSetRow(id, eventType, revision, occurredOn, data));
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

        final List<PlaceDomainEvent> events = new ArrayList<>(rsRows.size());

        try
        {
            for (final ResultSetRow row : rsRows)
            {
                final PlaceDomainEvent event = switch (row.eventType)
                {
                    case "place_created_event" -> objectMapper.readValue(row.data, PlaceCreatedEvent.class);
                    case "place_selected_event" -> objectMapper.readValue(row.data, PlaceSelectedEvent.class);
                    case "place_released_event" -> objectMapper.readValue(row.data, PlaceReleasedEvent.class);
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

        return Optional.of(Place.restore(placeId, events));
    }

    @Override
    public List<Place> findAll(final Collection<PlaceId> placeIds)
    {
        final Map<UUID, List<ResultSetRow>> rsRows = new HashMap<>(placeIds.size());

        final Object[] ids = placeIds.stream().map(PlaceId::toString).toArray();

        final Connection connection = transactionManager.currentConnection();

        try (final PreparedStatement statement = connection.prepareStatement("SELECT * FROM events WHERE aggregate_id = ANY(?)"))
        {
            final Array array = connection.createArrayOf("uuid", ids);
            statement.setArray(1, array);

            try (final ResultSet rs = statement.executeQuery())
            {
                if (rs.next())
                {
                    do
                    {
                        final UUID id = (UUID) rs.getObject("aggregate_id");
                        final String eventType = rs.getString("event_type");
                        final long revision = rs.getLong("revision");
                        final Instant occurredOn = rs.getObject("event_date", OffsetDateTime.class).toInstant();

                        final PGobject pGobject = (PGobject) rs.getObject("data");
                        final String data = pGobject.getValue();

                        final ResultSetRow row = new ResultSetRow(id, eventType, revision, occurredOn, data);
                        rsRows.compute(id, (k, v) ->
                        {
                            if (v == null)
                            {
                                final List<ResultSetRow> rows = new ArrayList<>();
                                rows.add(row);
                                return rows;
                            }
                            else
                            {
                                v.add(row);
                                return v;
                            }
                        });
                    }
                    while (rs.next());
                }
                else
                {
                    return List.of();
                }
            }
        }
        catch (final SQLException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }

        final Map<PlaceId, List<PlaceDomainEvent>> events = new HashMap<>(rsRows.size());

        try
        {
            for (final Map.Entry<UUID, List<ResultSetRow>> entry : rsRows.entrySet())
            {
                final PlaceId placeId = new PlaceId(entry.getKey());

                for (final ResultSetRow row : entry.getValue())
                {
                    final PlaceDomainEvent event = switch (row.eventType)
                    {
                        case "place_created_event" -> objectMapper.readValue(row.data, PlaceCreatedEvent.class);
                        case "place_selected_event" -> objectMapper.readValue(row.data, PlaceSelectedEvent.class);
                        case "place_released_event" -> objectMapper.readValue(row.data, PlaceReleasedEvent.class);
                        default -> throw new RuntimeException("Unknown event type: " + row.eventType);
                    };

                    events.compute(placeId, (k, v) ->
                    {
                        if (v == null)
                        {
                            final List<PlaceDomainEvent> rows = new ArrayList<>();
                            rows.add(event);
                            return rows;
                        }
                        else
                        {
                            v.add(event);
                            return v;
                        }
                    });
                }
            }

            return events.entrySet()
                    .stream()
                    .map((entry) -> Place.restore(entry.getKey(), entry.getValue()))
                    .toList();
        }
        catch (final JsonProcessingException e)
        {
            // todo: replace with domain exception
            throw new RuntimeException(e);
        }
    }
}
