package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;

public final class OutboxScheduler
{
    private static final Logger LOG = LoggerFactory.getLogger(OutboxScheduler.class);

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1);

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

    public void start()
    {
        scheduler.scheduleAtFixedRate(this::sendScheduledMessages, 100L, 100L, TimeUnit.MILLISECONDS);
    }

    public void stop()
    {
        scheduler.shutdown();

        try
        {
            final boolean terminated = scheduler.awaitTermination(1L, TimeUnit.MINUTES);
            if (!terminated)
            {
                LOG.warn("Tasks were not terminated as expected");
                scheduler.shutdownNow();
            }
        }
        catch (final InterruptedException e)
        {
            LOG.error(e.getMessage(), e);

            Thread.currentThread().interrupt();
        }
    }

    public void sendScheduledMessages()
    {
        transactionManager.executeInTransaction(() ->
        {
            final Optional<OutboxMessage> optionalOutboxMessage = outboxRepository.nextForDelivery();

            if (optionalOutboxMessage.isEmpty())
            {
                return;
            }

            final OutboxMessage outboxMessage = optionalOutboxMessage.get();
            outboxSender.send(outboxMessage.topic(), outboxMessage.aggregateId(), outboxMessage.payload());
            outboxRepository.delete(outboxMessage.id());
        });
    }
}
