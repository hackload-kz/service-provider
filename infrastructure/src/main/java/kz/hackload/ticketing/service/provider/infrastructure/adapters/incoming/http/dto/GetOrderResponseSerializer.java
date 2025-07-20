package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class GetOrderResponseSerializer extends JsonSerializer<GetOrderResponse>
{
    @Override
    public void serialize(final GetOrderResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("id", value.id().toString());
        gen.writeStringField("status", value.orderStatus().name());
        gen.writeStringField("started_at", DateTimeFormatter.ISO_INSTANT.format(value.startedAt().truncatedTo(ChronoUnit.SECONDS)));
        gen.writeStringField("updated_at", DateTimeFormatter.ISO_INSTANT.format(value.updatedAt().truncatedTo(ChronoUnit.SECONDS)));
        gen.writeNumberField("places_count", value.placesCount());
        gen.writeEndObject();
    }
}
