package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public final class GetPlaceResponseSerializer extends JsonSerializer<GetPlaceResponse>
{
    @Override
    public void serialize(final GetPlaceResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("id", value.placeId().toString());
        gen.writeNumberField("row", value.row().number());
        gen.writeNumberField("seat", value.seat().number());
        gen.writeBooleanField("is_free", value.isFree());
        gen.writeEndObject();
    }
}
