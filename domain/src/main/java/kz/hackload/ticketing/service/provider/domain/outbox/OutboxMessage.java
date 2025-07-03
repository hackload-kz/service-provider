package kz.hackload.ticketing.service.provider.domain.outbox;

import java.util.Objects;

public final class OutboxMessage
{
    private final OutboxMessageId id;
    private final String aggregateId;
    private final String aggregateType;
    private final String payload;

    public OutboxMessage(final OutboxMessageId id, final String aggregateId, final String aggregateType, final String payload)
    {
        this.id = Objects.requireNonNull(id);
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.aggregateType = Objects.requireNonNull(aggregateType);
        this.payload = Objects.requireNonNull(payload);
    }

    public OutboxMessageId id()
    {
        return id;
    }

    public String aggregateId()
    {
        return aggregateId;
    }

    public String aggregateType()
    {
        return aggregateType;
    }

    public String payload()
    {
        return payload;
    }
}
