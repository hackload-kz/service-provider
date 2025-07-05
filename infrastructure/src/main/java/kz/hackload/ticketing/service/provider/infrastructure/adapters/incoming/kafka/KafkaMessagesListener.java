package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.kafka.clients.consumer.KafkaConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class KafkaMessagesListener
{
    private static final Logger log = LoggerFactory.getLogger(KafkaMessagesListener.class);
    private final Map<String, DomainEventsListener> listeners = new ConcurrentHashMap<>();

    private final KafkaConsumer<String, String> consumer;
    private final Thread consumerThread;

    private volatile boolean running;

    public KafkaMessagesListener(final KafkaConsumer<String, String> consumer,
                                 final List<String> topics)
    {
        this.consumer = consumer;
        this.consumer.subscribe(topics);

        consumerThread = new Thread(this::consume, "kafka-events-consumer");
    }

    public void registerDomainEventsListener(final DomainEventsListener listener)
    {
        listeners.put(listener.topic(), listener);
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
                final String topic = record.topic();
                try
                {
                    listeners.get(topic).hande(record);
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
