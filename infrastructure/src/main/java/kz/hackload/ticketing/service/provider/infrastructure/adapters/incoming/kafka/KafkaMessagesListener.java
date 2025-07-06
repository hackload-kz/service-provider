package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.time.Duration;
import java.util.List;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaMessagesListener
{
    private static final Logger log = LoggerFactory.getLogger(KafkaMessagesListener.class);

    private final DomainEventsListener listener;
    private final KafkaConsumer<String, String> consumer;
    private final Thread consumerThread;

    private volatile boolean running;

    public KafkaMessagesListener(final KafkaConsumer<String, String> consumer,
                                 final String topic,
                                 final DomainEventsListener listener)
    {
        this.consumer = consumer;
        this.consumer.subscribe(List.of(topic));
        this.listener = listener;

        consumerThread = new Thread(this::consume, "kafka-events-consumer");
    }

    public void start()
    {
        running = true;
        consumerThread.start();
    }

    public void consume()
    {
        while (running && !Thread.currentThread().isInterrupted())
        {
            consumer.poll(Duration.ofMillis(10L)).forEach(record ->
            {
                try
                {
                    listener.hande(record);
                }
                catch (final RuntimeException e)
                {
                    log.error(e.getMessage(), e);
                }
            });
        }

        consumerThread.start();
    }

    public void stop()
    {
        running = false;
        consumerThread.interrupt();
    }
}
