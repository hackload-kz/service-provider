package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public final class CreatePlaceResponseSerializer extends JsonSerializer<CreatePlaceResponse>
{
    @Override
    public void serialize(final CreatePlaceResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("place_id", value.placeId().toString());
        gen.writeEndObject();
    }
}
