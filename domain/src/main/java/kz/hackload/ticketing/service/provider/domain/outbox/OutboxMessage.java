package kz.hackload.ticketing.service.provider.domain.outbox;

public record OutboxMessage(
        OutboxMessageId id,
        String topic,
        String aggregateId,
        long aggregateRevision,
        String aggregateType,
        String eventType,
        String payload)
{
}
