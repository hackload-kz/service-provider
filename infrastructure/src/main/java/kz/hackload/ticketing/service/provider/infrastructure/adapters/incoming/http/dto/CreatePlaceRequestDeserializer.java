package kz.hackload.ticketing.service.provider.infrastructure.adapters.incoming.http.dto;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

import kz.hackload.ticketing.service.provider.domain.places.Row;
import kz.hackload.ticketing.service.provider.domain.places.Seat;

public final class CreatePlaceRequestDeserializer extends JsonDeserializer<CreatePlaceRequest>
{
    @Override
    public CreatePlaceRequest deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException, JacksonException
    {
        final TreeNode treeNode = p.getCodec().readTree(p);

        final Row row = new Row(((JsonNode) treeNode.get("row")).asInt());
        final Seat seat = new Seat(((JsonNode) treeNode.get("seat")).asInt());

        return new CreatePlaceRequest(row, seat);
    }
}
