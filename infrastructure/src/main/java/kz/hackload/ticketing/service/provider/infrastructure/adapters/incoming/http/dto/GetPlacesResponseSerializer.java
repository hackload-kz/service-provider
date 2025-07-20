package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public final class GetPlacesResponseSerializer extends JsonSerializer<GetPlacesResponse>
{
    @Override
    public void serialize(final GetPlacesResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartArray();
        for (final GetPlacesResponse.Place place : value.places())
        {
            gen.writeStartObject();
            gen.writeStringField("id", place.placeId().toString());
            gen.writeNumberField("row", place.row().number());
            gen.writeNumberField("seat", place.seat().number());
            gen.writeBooleanField("is_free", place.isFree());
            gen.writeEndObject();
        }
        gen.writeEndArray();
    }
}
