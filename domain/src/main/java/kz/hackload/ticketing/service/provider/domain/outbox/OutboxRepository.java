package kz.hackload.ticketing.service.provider.domain.outbox;

import java.util.Optional;

public interface OutboxRepository
{
    OutboxMessageId nextId();

    void save(final OutboxMessage outboxMessage);

    Optional<OutboxMessage> nextForDelivery();

    void delete(final OutboxMessageId id);
}
