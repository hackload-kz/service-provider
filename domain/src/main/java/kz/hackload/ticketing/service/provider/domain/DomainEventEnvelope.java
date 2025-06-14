package kz.hackload.ticketing.service.provider.domain;

public record DomainEventEnvelope<IDENTITY extends DomainEntityId, ET extends DomainEvent>(
        IDENTITY aggregateId,
        long version,
        ET event)
{
}
