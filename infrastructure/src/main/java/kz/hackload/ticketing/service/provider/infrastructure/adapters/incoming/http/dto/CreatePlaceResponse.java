package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;

@JsonSerialize(using = CreatePlaceResponseSerializer.class)
public record CreatePlaceResponse(PlaceId placeId)
{
}
