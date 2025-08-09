package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaMessagesListener
{
    private static final Logger LOG = LoggerFactory.getLogger(KafkaMessagesListener.class);

    private final ScheduledExecutorService scheduler = new ScheduledThreadPoolExecutor(1, new ThreadFactory()
    {
        private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

        @Override
        public Thread newThread(final Runnable r)
        {
            return new Thread(r, "kafka-message-listener-" + THREAD_COUNTER.getAndDecrement());
        }
    });

    private static final AtomicLong COUNTER = new AtomicLong(0);
    private static final Duration POLL_INTERVAL = Duration.ofMillis(1L);

    private final KafkaConsumer<String, String> consumer;
    private final String topic;
    private final DomainEventsListener listener;

    public KafkaMessagesListener(final KafkaConsumer<String, String> consumer,
                                 final String topic,
                                 final DomainEventsListener listener)
    {
        this.consumer = consumer;
        this.consumer.subscribe(List.of(topic));
        this.topic = topic;
        this.listener = listener;
    }

    public void start()
    {
        LOG.info("Starting kafka message listener {}", topic);
        scheduler.scheduleAtFixedRate(this::consume, 0L, 10_000_000L, TimeUnit.NANOSECONDS);
    }

    public void consume()
    {
        try
        {
            for (ConsumerRecord<String, String> stringStringConsumerRecord : consumer.poll(POLL_INTERVAL))
            {
//                LOG.info("Message#{} is {}", COUNTER.incrementAndGet(), stringStringConsumerRecord);
                listener.hande(stringStringConsumerRecord);
            }
            consumer.commitSync();
        }
        catch (final RuntimeException e)
        {
            LOG.error("Consumer encountered with an error. It is stopped!", e);
            stop();
        }
    }

    public void stop()
    {
        LOG.info("Stopping kafka message listener {}", topic);
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
        catch (InterruptedException e)
        {
            LOG.error(e.getMessage(), e);
            Thread.currentThread().interrupt();
        }

        LOG.info("Kafka message listener {} is stopped", topic);
    }
}
