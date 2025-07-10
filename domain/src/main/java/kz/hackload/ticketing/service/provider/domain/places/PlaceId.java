package kz.hackload.ticketing.service.provider.domain.places;

import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.DomainEntityId;

public record PlaceId(UUID value) implements DomainEntityId<UUID>
{
    @Override
    public String toString()
    {
        return value.toString();
    }
}
