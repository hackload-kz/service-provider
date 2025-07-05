package kz.hackload.ticketing.service.provider.domain.outbox;

import java.util.Objects;

public record OutboxMessage(OutboxMessageId id, String topic, String aggregateId, String aggregateType, String payload)
{
    public OutboxMessage(final OutboxMessageId id, final String topic, final String aggregateId, final String aggregateType, final String payload)
    {
        this.id = Objects.requireNonNull(id);
        this.topic = topic;
        this.aggregateId = Objects.requireNonNull(aggregateId);
        this.aggregateType = Objects.requireNonNull(aggregateType);
        this.payload = Objects.requireNonNull(payload);
    }
}
