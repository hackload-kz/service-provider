package kz.hackload.ticketing.service.provider.application;

public interface JsonMapper
{
    <T> String toJson(T object);

    <T> T fromJson(String json, Class<T> clazz);
}
