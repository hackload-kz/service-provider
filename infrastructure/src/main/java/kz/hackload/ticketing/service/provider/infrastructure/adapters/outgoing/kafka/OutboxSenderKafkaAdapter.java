package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.kafka;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import kz.hackload.ticketing.service.provider.application.OutboxSender;

public final class OutboxSenderKafkaAdapter implements OutboxSender
{
    private static final Logger log = LoggerFactory.getLogger(OutboxSenderKafkaAdapter.class);
    private final KafkaProducer<String, String> producer;

    public OutboxSenderKafkaAdapter(final KafkaProducer<String, String> producer)
    {
        this.producer = producer;
    }

    @Override
    public void send(final String destination, final String routingKey, final String eventType, final String payload)
    {
        try
        {
            final ProducerRecord<String, String> record = new ProducerRecord<>(destination, routingKey, payload);
            record.headers().add(new RecordHeader("event_type", eventType.getBytes(StandardCharsets.UTF_8)));

            producer.send(record).get();
            log.info("Message sent to {} with routing key {} and payload {}", destination, routingKey, payload);
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
