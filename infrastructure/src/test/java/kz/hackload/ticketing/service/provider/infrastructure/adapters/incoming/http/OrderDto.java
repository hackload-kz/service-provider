package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;
import java.time.Instant;
import java.util.UUID;

import kz.hackload.ticketing.service.provider.domain.orders.OrderId;
import kz.hackload.ticketing.service.provider.domain.orders.OrderStatus;

@JsonDeserialize(using = OrderDto.OrderDtoDeserializer.class)
public record OrderDto(OrderId id, OrderStatus status, Instant startedAt, Instant updatedAt, long placesCount)
{
    public static final class OrderDtoDeserializer extends JsonDeserializer<OrderDto>
    {
        @Override
        public OrderDto deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException
        {
            final TreeNode treeNode = p.getCodec().readTree(p);
            final OrderId orderId = new OrderId(UUID.fromString(((JsonNode) treeNode.get("id")).asText()));
            final OrderStatus orderStatus = OrderStatus.valueOf(((JsonNode) treeNode.get("status")).asText());
            final Instant startedAt = Instant.ofEpochMilli(((JsonNode) treeNode.get("started_at")).asLong());
            final Instant updatedAt = Instant.ofEpochMilli(((JsonNode) treeNode.get("updated_at")).asLong());
            final long placesCount = ((JsonNode) treeNode.get("places_count")).asLong();

            return new OrderDto(orderId, orderStatus, startedAt, updatedAt, placesCount);
        }
    }
}
