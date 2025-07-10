package kz.hackload.ticketing.service.provider.domain.places;

public record GetPlaceQueryResult(
        PlaceId placeId,
        Row row,
        Seat seat,
        boolean isFree)
{
}
