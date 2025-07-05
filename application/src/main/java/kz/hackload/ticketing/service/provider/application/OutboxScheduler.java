package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;

public final class OutboxScheduler
{
    private final TransactionManager transactionManager;
    private final OutboxRepository outboxRepository;
    private final OutboxSender outboxSender;

    public OutboxScheduler(final TransactionManager transactionManager,
                           final OutboxRepository outboxRepository,
                           final OutboxSender outboxSender)
    {
        this.transactionManager = transactionManager;
        this.outboxRepository = outboxRepository;
        this.outboxSender = outboxSender;
    }

    public void sendScheduledMessages()
    {
        final AtomicBoolean needContinue = new AtomicBoolean(true);

        do
        {
            transactionManager.executeInTransaction(() ->
            {
                final Optional<OutboxMessage> optionalOutboxMessage = outboxRepository.nextForDelivery();

                if (optionalOutboxMessage.isEmpty())
                {
                    needContinue.set(false);
                    return;
                }

                final OutboxMessage outboxMessage = optionalOutboxMessage.get();
                outboxSender.send(outboxMessage.topic(), outboxMessage.aggregateId(), outboxMessage.payload());
                outboxRepository.delete(outboxMessage.id());
            });
        }
        while (needContinue.get());
    }
}
