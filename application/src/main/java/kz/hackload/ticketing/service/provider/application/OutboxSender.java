package kz.hackload.ticketing.service.provider.application;

public interface OutboxSender
{
    void send(final String destination, String routingKey, final String payload);
}
