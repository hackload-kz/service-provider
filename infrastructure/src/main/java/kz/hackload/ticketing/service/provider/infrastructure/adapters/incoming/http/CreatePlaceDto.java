package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

@JsonDeserialize(using = CreatePlaceDtoDeserializer.class)
public record CreatePlaceDto(Row row, Seat seat)
{
}
