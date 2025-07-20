package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;

public final class SelectPlaceDtoDeserializer extends JsonDeserializer<SelectPlaceDto>
{
    @Override
    public SelectPlaceDto deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException
    {
        final TreeNode treeNode = p.getCodec().readTree(p);

        final OrderId orderId = new OrderId(UUID.fromString(((JsonNode)treeNode.get("order_id")).asText()));

        return new SelectPlaceDto(orderId);
    }
}
