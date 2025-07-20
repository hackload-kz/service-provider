package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

@JsonDeserialize(using = CreatePlaceRequestDeserializer.class)
public record CreatePlaceRequest(Row row, Seat seat)
{
}
