package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

@JsonSerialize(using = GetPlaceResponseSerializer.class)
public record GetPlaceResponse(PlaceId placeId, Row row, Seat seat, boolean isFree)
{
}
