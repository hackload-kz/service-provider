package kz.hackload.ticketing.service.provider.domain.orders;

import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.DomainEntityId;

public record OrderId(UUID value) implements DomainEntityId<UUID>
{
}
