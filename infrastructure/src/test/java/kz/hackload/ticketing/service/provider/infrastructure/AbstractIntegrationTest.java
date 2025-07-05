package kz.hackload.ticketing.service.provider.infrastructure;

import kz.hackload.ticketing.service.provider.application.JsonMapper;
import kz.hackload.ticketing.service.provider.infrastructure.adapters.JacksonJsonMapper;

public abstract class AbstractIntegrationTest
{
    protected JsonMapper jsonMapper = new JacksonJsonMapper();
}
