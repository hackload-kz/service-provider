package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

@JsonDeserialize(using = ReleasePlaceDtoDeserializer.class)
public record ReleasePlaceDto(PlaceId placeId)
{
}
