package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public record PlaceTestDto(PlaceId placeId, Row row, Seat seat, boolean isFree) {}
