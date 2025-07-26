package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public final class GetOrderResponseSerializer extends JsonSerializer<GetOrderResponse>
{
    @Override
    public void serialize(final GetOrderResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("id", value.id().toString());
        gen.writeStringField("status", value.orderStatus().name());
        gen.writeNumberField("started_at", value.startedAt().toEpochMilli());
        gen.writeNumberField("updated_at", value.updatedAt().toEpochMilli());
        gen.writeNumberField("places_count", value.placesCount());
        gen.writeEndObject();
    }
}
