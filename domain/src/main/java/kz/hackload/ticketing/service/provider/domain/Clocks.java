package kz.hackload.ticketing.service.provider.domain;

import java.time.Instant;

public interface Clocks
{
    Instant now();
}
