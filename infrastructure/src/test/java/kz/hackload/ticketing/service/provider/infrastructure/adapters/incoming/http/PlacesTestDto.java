package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.places.PlaceId;
import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

@JsonDeserialize(using = PlacesTestDto.PlacesTestDtoDeserializer.class)
public record PlacesTestDto(List<PlaceTestDto> places)
{
    public record PlaceTestDto(PlaceId placeId, Row row, Seat seat, boolean isFree)
    {
    }

    public static final class PlacesTestDtoDeserializer extends JsonDeserializer<PlacesTestDto>
    {
        @Override
        public PlacesTestDto deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException
        {
            final List<PlaceTestDto> placeTestDtos = new ArrayList<>();

            final TreeNode root = p.getCodec().readTree(p);

            for (int i = 0; i < root.size(); i++)
            {
                final TreeNode place = root.get(i);

                final PlaceId placeId = new PlaceId(UUID.fromString(((JsonNode) place.get("id")).asText()));
                final Row row = new Row(((JsonNode) place.get("row")).asInt());
                final Seat seat = new Seat(((JsonNode) place.get("seat")).asInt());
                final boolean isFree = ((JsonNode) place.get("is_free")).asBoolean();

                placeTestDtos.add(new PlaceTestDto(placeId, row, seat, isFree));
            }

            return new PlacesTestDto(placeTestDtos);
        }
    }
}
