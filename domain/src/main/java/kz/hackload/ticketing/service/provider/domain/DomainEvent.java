package kz.hackload.ticketing.service.provider.domain;

import java.time.Instant;

public interface DomainEvent
{
    Instant occurredOn();

    long revision();

    String type();
}
