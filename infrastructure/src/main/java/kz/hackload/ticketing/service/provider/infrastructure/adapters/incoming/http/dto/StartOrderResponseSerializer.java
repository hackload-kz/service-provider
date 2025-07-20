package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

public final class StartOrderResponseSerializer extends JsonSerializer<StartOrderResponse>
{
    @Override
    public void serialize(final StartOrderResponse value, final JsonGenerator gen, final SerializerProvider serializers) throws IOException
    {
        gen.writeStartObject();
        gen.writeStringField("order_id", value.orderId().toString());
        gen.writeEndObject();
    }
}
