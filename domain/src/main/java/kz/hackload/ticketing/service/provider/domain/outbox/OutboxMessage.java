package kz.hackload.ticketing.service.provider.domain.outbox;

import java.time.Instant;

public record OutboxMessage(
        OutboxMessageId id,
        String topic,
        String aggregateId,
        Instant occurredOn,
        String aggregateType,
        String eventType,
        String payload)
{
}
