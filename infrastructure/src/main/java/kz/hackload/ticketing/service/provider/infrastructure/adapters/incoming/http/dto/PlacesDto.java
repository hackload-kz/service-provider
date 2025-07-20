package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import java.util.List;

import kz.hackload.ticketing.service.provider.domain.places.GetPlaceQueryResult;

public record PlacesDto(List<GetPlaceQueryResult> places)
{
}
