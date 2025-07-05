package kz.hackload.ticketing.service.provider.infrastructure.adapters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kz.hackload.ticketing.service.provider.application.JsonMapper;

public final class JacksonJsonMapper implements JsonMapper
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <T> String toJson(final T object)
    {
        try
        {
            return objectMapper.writeValueAsString(object);
        }
        catch (final JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T fromJson(final String json, final Class<T> clazz)
    {
        try
        {
            return objectMapper.readValue(json, clazz);
        }
        catch (JsonProcessingException e)
        {
            throw new RuntimeException(e);
        }
    }
}
