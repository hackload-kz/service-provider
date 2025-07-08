package kz.hackload.ticketing.service.provider.domain;

import java.time.Instant;

public final class RealClock implements Clocks
{
    @Override
    public Instant now()
    {
        return Instant.now();
    }
}
