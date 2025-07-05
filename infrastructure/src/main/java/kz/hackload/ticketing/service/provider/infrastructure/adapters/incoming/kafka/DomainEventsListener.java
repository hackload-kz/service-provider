package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;

public interface DomainEventsListener
{
    String topic();

    void hande(final ConsumerRecord<String, String> record);
}
