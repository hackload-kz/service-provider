package kz.hackload.ticketing.service.provider.application;

public interface OutboxSender
{
    void send(final String destination,
              final String routingKey,
              final String eventType,
              final String payload);
}
