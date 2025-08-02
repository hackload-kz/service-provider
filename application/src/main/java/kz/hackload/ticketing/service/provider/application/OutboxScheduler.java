package kz.hackload.ticketing.service.provider.application;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxMessage;
import kz.hackload.ticketing.service.provider.domain.outbox.OutboxRepository;

public final class OutboxScheduler
{
    private static final Logger LOG = LoggerFactory.getLogger(OutboxScheduler.class);

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(2, new ThreadFactory()
    {
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

        @Override
        public Thread newThread(final Runnable r)
        {
            return new Thread(r, "outbox-scheduler-" + THREAD_COUNTER.getAndDecrement());
        }
    });

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
        LOG.info("Starting outbox scheduler");
        scheduler.scheduleAtFixedRate(this::sendScheduledOrders, 10L, 10L, TimeUnit.MILLISECONDS);
        scheduler.scheduleAtFixedRate(this::sendScheduledPlaces, 10L, 10L, TimeUnit.MILLISECONDS);
    }

    public void stop()
    {
        LOG.info("Stopping outbox scheduler");

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

        LOG.info("Outbox scheduler is stopped");
    }

    @WithSpan(kind = SpanKind.INTERNAL, inheritContext = false)
    public void sendScheduledOrders()
    {
        try
        {
            transactionManager.executeInTransaction(() ->
            {
                final Optional<OutboxMessage> optionalOutboxMessage = outboxRepository.nextForDelivery("order");

                if (optionalOutboxMessage.isEmpty())
                {
                    return;
                }

                final OutboxMessage outboxMessage = optionalOutboxMessage.get();
                outboxSender.send(outboxMessage.topic(), outboxMessage.aggregateId(), outboxMessage.eventType(), outboxMessage.payload());
                outboxRepository.delete(outboxMessage.id());
            });
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
        }
    }

    @WithSpan(kind = SpanKind.INTERNAL, inheritContext = false)
    public void sendScheduledPlaces()
    {
        try
        {
            transactionManager.executeInTransaction(() ->
            {
                final Optional<OutboxMessage> optionalOutboxMessage = outboxRepository.nextForDelivery("place");

                if (optionalOutboxMessage.isEmpty())
                {
                    return;
                }

                final OutboxMessage outboxMessage = optionalOutboxMessage.get();
                outboxSender.send(outboxMessage.topic(), outboxMessage.aggregateId(), outboxMessage.eventType(), outboxMessage.payload());
                outboxRepository.delete(outboxMessage.id());
            });
        }
        catch (final RuntimeException e)
        {
            LOG.error(e.getMessage(), e);
        }
    }
}
