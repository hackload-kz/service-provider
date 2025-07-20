package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import java.util.List;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

@JsonSerialize(using = GetPlacesResponseSerializer.class)
public record GetPlacesResponse(List<Place> places)
{
    public record Place(PlaceId placeId, Row row, Seat seat, boolean isFree) {}
}
