package kz.hackload.ticketing.service.provider.infrastructure.adapters.outgoing.jdbc;

import java.util.concurrent.ExecutionException;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import kz.hackload.ticketing.service.provider.application.OutboxSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OutboxSenderKafkaAdapter implements OutboxSender
{
    private static final Logger log = LoggerFactory.getLogger(OutboxSenderKafkaAdapter.class);
    private final KafkaProducer<String, String> producer;

    public OutboxSenderKafkaAdapter(final KafkaProducer<String, String> producer)
    {
        this.producer = producer;
    }

    @Override
    public void send(final String destination, final String routingKey, final String payload)
    {
        try
        {
            producer.send(new ProducerRecord<>(destination, routingKey, payload)).get();
            log.info("Message sent to {} with routing key {} and payload {}", destination, routingKey, payload);
        }
        catch (InterruptedException e)
        {
            throw new RuntimeException(e);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
    }
}
